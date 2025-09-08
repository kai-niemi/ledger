package io.cockroachdb.ledger.repository.jdbc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.cockroachdb.ledger.domain.SurvivalGoal;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.repository.MultiRegionRepository;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class JdbcMultiRegionRepository implements MultiRegionRepository {
    private static String formatSQL(String sql, String... args) {
        Pattern pattern = Pattern.compile("^[\\w-]+$");
        Arrays.stream(args).forEach(p -> {
            if (!pattern.matcher(p).matches()) {
                throw new IllegalArgumentException("Not matching a word: " + p);
            }
        });
        return sql.formatted((Object[]) args);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void addDatabaseRegions(List<Region> regions) {
        final List<Region> validRegions = new ArrayList<>();

        regions.stream()
                .filter(r -> !r.getDatabaseRegions().isEmpty())
                .forEach(validRegions::add);

        validRegions.stream()
                .filter(Region::isPrimary)
                .findFirst()
                .ifPresent(region -> {
                    String primary = region.getDatabaseRegionSingleton();
                    jdbcTemplate.update(formatSQL(
                            "ALTER DATABASE ledger PRIMARY REGION '%s'", primary));
                });

        validRegions.forEach(region -> {
            if (!region.isPrimary()) {
                jdbcTemplate.update(formatSQL("ALTER DATABASE ledger ADD REGION IF NOT EXISTS '%s'",
                        region.getDatabaseRegionSingleton()));

                if (region.isSecondary()) {
                    jdbcTemplate.update(formatSQL("ALTER DATABASE ledger SET SECONDARY REGION '%s'",
                            region.getDatabaseRegionSingleton()));
                }
            }
        });
    }

    @Override
    public void dropDatabaseRegions(List<Region> regions) {
        regions.stream()
                .filter(region -> !region.getDatabaseRegions().isEmpty())
                .forEach(region -> {
                    if (!region.isPrimary()) {
                        jdbcTemplate.update(formatSQL(
                                "ALTER DATABASE ledger DROP REGION IF EXISTS '%s'",
                                region.getDatabaseRegionSingleton()));
                    }
                    if (region.isSecondary()) {
                        dropSecondaryRegion();
                    }
                });
    }

    @Override
    public void setPrimaryRegion(Region region) {
        jdbcTemplate.update(formatSQL("ALTER DATABASE ledger SET PRIMARY REGION '%s'",
                region.getDatabaseRegionSingleton()));
    }

    @Override
    public void setSecondaryRegion(Region region) {
        jdbcTemplate.update(formatSQL("ALTER DATABASE ledger SET SECONDARY REGION '%s'",
                region.getDatabaseRegionSingleton()));
    }

    @Override
    public void dropSecondaryRegion() {
        jdbcTemplate.update("ALTER DATABASE ledger DROP SECONDARY REGION");
    }

    @Override
    public void setSurvivalGoal(SurvivalGoal survivalGoal) {
        if (survivalGoal.equals(SurvivalGoal.REGION)) {
            jdbcTemplate.update("ALTER DATABASE ledger SURVIVE REGION FAILURE");
        } else {
            jdbcTemplate.update("ALTER DATABASE ledger SURVIVE ZONE FAILURE");
        }
    }

    @Override
    public Optional<String> getSurvivalGoal() {
        try {
            String goal = jdbcTemplate.queryForObject(
                    "select survival_goal from [show databases] where database_name='ledger'",
                    String.class);
            return Optional.ofNullable(goal);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> showCreateTable(String table) {
        try {
            String goal = jdbcTemplate.queryForObject("select create_statement from [show create table %s]"
                    .formatted(table), String.class);
            return Optional.ofNullable(goal);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void setGlobalTable(String table) {
        jdbcTemplate.update(formatSQL("ALTER TABLE %s SET locality GLOBAL", table));
    }

    @Override
    public void setRegionalByTable(String table) {
        jdbcTemplate.update(formatSQL("ALTER TABLE %s SET locality REGIONAL BY TABLE IN PRIMARY REGION",
                table));
    }

    @Override
    public void dropRegionColumn(String table) {
        jdbcTemplate.update(formatSQL("ALTER TABLE %s DROP COLUMN IF EXISTS region",
                table));
    }

    @Override
    public void setRegionalByRowTable(List<Region> regions, String table) {
        final StringBuilder sb = new StringBuilder()
                .append("ALTER TABLE ")
                .append(table)
                .append(" ADD COLUMN IF NOT EXISTS region crdb_internal_region AS (CASE");

        Deque<Region> primary = new ArrayDeque<>();

        regions
                .stream()
                .filter(region -> !region.getDatabaseRegions().isEmpty())
                .forEach(region -> {
                    if (region.isPrimary()) {
                        primary.push(region);
                    }

                    sb.append(" WHEN city IN (");

                    boolean sep = false;

                    for (City city : region.getCities()) {
                        if (sep) {
                            sb.append(",");
                        }
                        sep = true;
                        sb.append("'").append(city.getName()).append("'");
                    }
                    sb.append(") THEN '")
                            .append(region.getDatabaseRegionSingleton())
                            .append("'");
                });

        if (!primary.isEmpty()) {
            if (primary.size() != 1) {
                throw new IllegalStateException("Expected one primary region, got " + primary.size());
            }

            sb.append(" ELSE '")
                    .append(primary.pop().getDatabaseRegionSingleton())
                    .append("' END) STORED NOT NULL");

            logger.debug("SQL: %s".formatted(sb.toString()));

            jdbcTemplate.execute(sb.toString());

            jdbcTemplate.update(formatSQL("ALTER TABLE %s SET LOCALITY REGIONAL BY ROW AS region", table));
        } else {
            throw new IllegalStateException("No primary region defined");
        }
    }
}
