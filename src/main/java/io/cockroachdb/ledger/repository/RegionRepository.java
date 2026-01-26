package io.cockroachdb.ledger.repository;

import java.util.List;
import java.util.Optional;

import io.cockroachdb.ledger.domain.ClusterInfo;

public interface RegionRepository {
    ClusterInfo clusterInfo();

    String databaseVersion();

    String databaseIsolation();

    List<String> listClusterRegions();

    List<String> listDatabaseRegions();

    Optional<String> getGatewayRegion();

    Optional<String> getPrimaryRegion();

    Optional<String> getSecondaryRegion();
}
