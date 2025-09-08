package io.cockroachdb.ledger.repository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository {
    String databaseVersion();

    String databaseIsolation();

    List<String> listClusterRegions();

    List<String> listDatabaseRegions();

    Optional<String> getGatewayRegion();

    Optional<String> getPrimaryRegion();

    Optional<String> getSecondaryRegion();
}
