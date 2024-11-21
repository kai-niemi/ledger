package se.cockroachdb.ledger.repository;

import java.util.List;
import java.util.Optional;

import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.model.SurvivalGoal;

public interface MultiRegionRepository {
    void addDatabaseRegions(List<Region> regions);

    void dropDatabaseRegions(List<Region> regions);

    void setPrimaryRegion(Region region);

    void setSecondaryRegion(Region region);

    void dropSecondaryRegion();

    void setSurvivalGoal(SurvivalGoal survivalGoal);

    Optional<String> getSurvivalGoal();

    void setGlobalTable(String table);

    void setRegionalByRowTable(List<Region> regions, String table);

    void setRegionalByTable(String table);

    Optional<String> showCreateTable(String table);

    void dropRegionColumn(String table) ;
}
