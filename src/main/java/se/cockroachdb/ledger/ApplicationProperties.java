package se.cockroachdb.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import se.cockroachdb.ledger.model.AccountPlan;
import se.cockroachdb.ledger.model.Region;

@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    private boolean usingLocks;

    private boolean idempotencyCheck;

    @NotNull
    private AccountPlan accountPlan;

    private Set<String> visibleRegions = new HashSet<>();

    @NotEmpty
    private List<Region> regions = new ArrayList<>();

    private Map<String, String> regionMappings = new HashMap<>();

    public Set<String> getVisibleRegions() {
        return visibleRegions;
    }

    public void setVisibleRegions(Set<String> visibleRegions) {
        this.visibleRegions = visibleRegions;
    }

    public AccountPlan getAccountPlan() {
        return accountPlan;
    }

    public void setAccountPlan(AccountPlan accountPlan) {
        this.accountPlan = accountPlan;
    }

    public boolean isUsingLocks() {
        return usingLocks;
    }

    public void setUsingLocks(boolean usingLocks) {
        this.usingLocks = usingLocks;
    }

    public boolean isIdempotencyCheck() {
        return idempotencyCheck;
    }

    public void setIdempotencyCheck(boolean idempotencyCheck) {
        this.idempotencyCheck = idempotencyCheck;
    }

    public List<Region> getRegions() {
        return regions.stream()
                .filter(region -> {
                    return visibleRegions.isEmpty()
                           || visibleRegions.contains(region.getName());
                }).toList();
    }

    public Optional<Region> getRegionByName(String name) {
        return regions.stream()
                .filter(r -> r.getName().equals(name)).findFirst();
    }

    public Optional<Region> getRegionWithDatabaseRegion(String region) {
        return regions.stream()
                .filter(r -> r.getDatabaseRegions().contains(region))
                .findFirst();
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

    public Map<String, String> getRegionMappings() {
        return regionMappings;
    }

    public void setRegionMappings(Map<String, String> regionMappings) {
        this.regionMappings = regionMappings;
    }

    @Override
    public String toString() {
        return "ApplicationModel{" +
               ", selectForUpdate=" + usingLocks +
               ", accountPlan=" + accountPlan +
               ", regions=" + regions +
               ", regionMappings=" + regionMappings +
               '}';
    }
}
