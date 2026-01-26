package io.cockroachdb.ledger.repository.jdbc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.cockroachdb.ledger.domain.ClusterInfo;
import io.cockroachdb.ledger.repository.RegionRepository;
import io.cockroachdb.ledger.util.MetadataUtils;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class JdbcDatabaseRepository implements RegionRepository {
    private static final String SQL_VCPU_TOTAL = """
            SELECT ceil((
                (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.user.percent')
                + (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.sys.percent'))
                * (SELECT value FROM crdb_internal.node_metrics WHERE name = 'liveness.livenodes')
                / (SELECT value FROM crdb_internal.node_metrics WHERE name = 'sys.cpu.combined.percent-normalized')
            ) AS vcpus
            """;

    private static final String SQL_CLUSTER_SUMMARY = """
            SELECT crdb_internal.cluster_id() as cluster_id,
                   count(node_id) as nodes,
                   min(build_tag) as min_ver,
                   max(build_tag) as max_ver
            FROM crdb_internal.gossip_nodes
            WHERE is_live = true
            """;

    private DataSource dataSource;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public ClusterInfo clusterInfo() {
        if (!MetadataUtils.isCockroachDB(dataSource)) {
            return ClusterInfo.withDefaults();
        }
        Integer vCPUs = jdbcTemplate.queryForObject(SQL_VCPU_TOTAL, Integer.class);
        return jdbcTemplate.queryForObject(SQL_CLUSTER_SUMMARY,
                (rs, rowNum) -> {
                    ClusterInfo clusterInfo = new ClusterInfo();
                    clusterInfo.setNumVCPUs(Math.max(1, vCPUs));
                    clusterInfo.setClusterId(rs.getString("cluster_id"));
                    clusterInfo.setNumNodes(Math.max(1, rs.getInt("nodes")));
                    clusterInfo.setMinVersion(rs.getString("min_ver"));
                    clusterInfo.setMaxVersion(rs.getString("max_ver"));
                    return clusterInfo;
                });
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

