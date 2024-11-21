package se.cockroachdb.ledger.repository.jdbc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import se.cockroachdb.ledger.repository.RegionRepository;
import se.cockroachdb.ledger.util.MetadataUtils;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class JdbcDatabaseRepository implements RegionRepository {
    private DataSource dataSource;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String databaseVersion() {
        return MetadataUtils.databaseVersion(dataSource);
    }

    @Override
    public String databaseIsolation() {
        return MetadataUtils.databaseIsolation(dataSource);
    }

    @Override
    public Optional<String> getGatewayRegion() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return Optional.empty();
        }

        String region = this.namedParameterJdbcTemplate
                .queryForObject("SELECT gateway_region()",
                        Collections.emptyMap(),
                        String.class);

        return Optional.ofNullable(region);
    }

    @Override
    public List<String> listClusterRegions() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        return this.namedParameterJdbcTemplate.query(
                "select region from [SHOW REGIONS]",
                parameters,
                (rs, rowNum) -> {
                    return rs.getString(1);
                });
    }

    @Override
    public List<String> listDatabaseRegions() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        return this.namedParameterJdbcTemplate.query(
                "select region from [SHOW REGIONS FROM DATABASE ledger]",
                parameters,
                (rs, rowNum) -> {
                    return rs.getString(1);
                });
    }

    @Override
    public Optional<String> getPrimaryRegion() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return Optional.empty();
        }

        try {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            String region = this.namedParameterJdbcTemplate.queryForObject(
                    "select region from [SHOW REGIONS FROM DATABASE ledger] WHERE \"primary\" = true",
                    parameters,
                    String.class);
            return Optional.ofNullable(region);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getSecondaryRegion() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return Optional.empty();
        }

        try {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            String region = this.namedParameterJdbcTemplate.queryForObject(
                    "select region from [SHOW REGIONS FROM DATABASE ledger] WHERE \"secondary\" = true",
                    parameters,
                    String.class);
            return Optional.ofNullable(region);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}

