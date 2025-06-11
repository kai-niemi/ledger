package se.cockroachdb.ledger.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import se.cockroachdb.ledger.model.ApplicationProperties;
import se.cockroachdb.ledger.annotations.ServiceFacade;
import se.cockroachdb.ledger.annotations.TransactionImplicit;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.model.RegionCategory;
import se.cockroachdb.ledger.model.SurvivalGoal;
import se.cockroachdb.ledger.repository.MultiRegionRepository;
import se.cockroachdb.ledger.repository.RegionRepository;

@ServiceFacade
public class RegionServiceFacade {
    private static List<String> RBR_TABLES = List.of(
            "account",
            "transfer",
            "transfer_item"
    );

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private MultiRegionRepository multiRegionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @PostConstruct
    public void init() {
        updateRegions();
    }

    private void updateRegions() {
        final List<Region> regions = applicationProperties.getRegions();
        final Map<String, String> regionMappings = applicationProperties.getRegionMappings();

        if (regions.isEmpty()) {
            logger.warn("No regions found - check application-<profile>.yml");
        }
        if (regionMappings.isEmpty()) {
            logger.warn("No region mappings found - check application-<profile>.yml");
        }

        final List<String> clusterRegions = regionRepository.listClusterRegions();

        if (clusterRegions.isEmpty()) {
            logger.warn("No cluster regions found!");
        }

        // Ensure all cities have a country and currency code
        regions.forEach(region -> {
            region.getCities().forEach(city -> {
                if (!StringUtils.hasLength(city.getCountry())) {
                    if (!StringUtils.hasLength(region.getCountry())) {
                        throw new ApplicationContextException(
                                "City '%s' in region '%s' missing country code".formatted(city.getName(),
                                        region.getName()));
                    }
                    city.setCountry(region.getCountry());
                }

                if (!StringUtils.hasLength(city.getCurrency())) {
                    if (!StringUtils.hasLength(region.getCurrency())) {
                        throw new ApplicationContextException(
                                "City '%s' in region '%s' missing currency code".formatted(city.getName(),
                                        region.getName()));
                    }
                    city.setCurrency(region.getCurrency());
                }

                try {
                    Currency.getInstance(city.getCurrency());
                } catch (IllegalArgumentException e) {
                    throw new ApplicationContextException(
                            "City '%s' currency code '%s' is invalid".formatted(city.getName(),
                                    city.getCurrency()), e);
                }
            });
        });

        // Map regions to actual cluster regions
        regions.forEach(region -> {
            region.clearDatabaseRegions();

            // Check if app region matches a current cluster region
            if (clusterRegions.contains(region.getName())) {
                region.addDatabaseRegion(region.getName());
            } else {
                // If not, check if there's a mapping
                regionMappings.entrySet().stream()
                        .filter(e -> e.getValue().equals(region.getName()))
                        .forEach(e -> {
                            if (clusterRegions.contains(e.getKey())) {
                                region.addDatabaseRegion(e.getKey());
                            }
                        });
            }
        });

        // Update region status flags based on current DB state
        if (!regionRepository.listDatabaseRegions().isEmpty()) {
            regionRepository.getPrimaryRegion().ifPresentOrElse(s -> {
                regions.forEach(region -> {
                    if (region.getDatabaseRegions().contains(s)) {
                        region.setPrimary(true);
                        logger.debug("Marked region '%s' as primary".formatted(region.getName()));
                    } else {
                        region.setPrimary(false);
                    }
                });
            }, () -> regions.forEach(region -> region.setPrimary(false)));

            regionRepository.getSecondaryRegion().ifPresentOrElse(s -> {
                regions.forEach(region -> {
                    if (region.getDatabaseRegions().contains(s)) {
                        region.setSecondary(true);
                        logger.debug("Marked region '%s' as secondary".formatted(region.getName()));
                    } else {
                        region.setSecondary(false);
                    }
                });
            }, () -> regions.forEach(region -> region.setSecondary(false)));
        }
    }

    @TransactionImplicit(readOnly = true)
    public String getDatabaseVersion() {
        return regionRepository.databaseVersion();
    }

    @TransactionImplicit(readOnly = true)
    public String getDatabaseIsolation() {
        return regionRepository.databaseIsolation();
    }

    @TransactionImplicit(readOnly = true)
    public List<Region> listRegions(String region) {
        RegionCategory regionCategory;

        try {
            regionCategory = RegionCategory.valueOf(region);
        } catch (IllegalArgumentException e) {
            Optional<Region> specified = applicationProperties.getRegionByName(region);
            return specified.map(List::of).orElseGet(List::of);
        }

        if (regionCategory == RegionCategory.all) {
            return applicationProperties.getRegions();
        } else {
            Optional<String> singleton = switch (regionCategory) {
                case gateway -> regionRepository.getGatewayRegion();
                case primary -> regionRepository.getPrimaryRegion();
                case secondary -> regionRepository.getSecondaryRegion();
                default -> throw new IllegalStateException("Unexpected value: " + regionCategory);
            };

            if (singleton.isPresent()) {
                Optional<Region> gatewayRegion = applicationProperties
                        .getRegionWithDatabaseRegion(singleton.get());
                return gatewayRegion.map(List::of).orElseGet(List::of);
            }
        }

        return List.of();
    }

    @TransactionImplicit(readOnly = true)
    public List<Region> listAllRegions() {
        final List<Region> regions = new ArrayList<>(applicationProperties.getRegions());

        // Move gateway region to top of list
        getGatewayRegion().ifPresent(region -> {
            Collections.swap(regions, regions.indexOf(region), 0);
        });

        return regions;
    }

    @TransactionImplicit
    public void applyMultiRegion(SurvivalGoal goal) {
        logger.info("Adding database regions: %s".formatted(
                        String.join(",", applicationProperties.getRegions()
                                .stream()
                                .filter(r -> !r.getDatabaseRegions().isEmpty())
                                .map(Region::getDatabaseRegion)
                                .collect(Collectors.toSet()))
                )
        );
        multiRegionRepository.addDatabaseRegions(applicationProperties.getRegions());

        logger.info("Adding RBR localities: %s".formatted(String.join(",", RBR_TABLES)));
        RBR_TABLES.forEach(table -> multiRegionRepository.setRegionalByRowTable(applicationProperties.getRegions(), table));

        logger.info("Setting survival goal: %s".formatted(goal));
        multiRegionRepository.setSurvivalGoal(goal);

        updateRegions();
    }

    @TransactionImplicit
    public void revertMultiRegion() {
        logger.info("Reverting table localities");

        RBR_TABLES.forEach(table -> multiRegionRepository.setRegionalByTable(table));

        logger.info("Dropping computed region columns");
        RBR_TABLES.forEach(table -> multiRegionRepository.dropRegionColumn(table));

        logger.info("Reverting survival goal to ZONE");
        multiRegionRepository.setSurvivalGoal(SurvivalGoal.ZONE);

        logger.info("Dropping regions");
        multiRegionRepository.dropDatabaseRegions(applicationProperties.getRegions());

        updateRegions();
    }

    @TransactionImplicit
    public void addDatabaseRegions() {
        List<Region> regions = applicationProperties.getRegions();
        multiRegionRepository.addDatabaseRegions(regions);

        updateRegions();
    }

    @TransactionImplicit
    public void dropDatabaseRegions() {
        List<Region> regions = applicationProperties.getRegions();
        multiRegionRepository.dropDatabaseRegions(regions);

        updateRegions();
    }

    @TransactionImplicit
    public void setPrimaryRegion(String region) {
        Region r = applicationProperties.getRegionByName(region)
                .orElseThrow(() -> new IllegalArgumentException("No such region: " + region));
        multiRegionRepository.setPrimaryRegion(r);

        updateRegions();
    }

    @TransactionImplicit
    public void setSecondaryRegion(String region) {
        Region r = applicationProperties.getRegionByName(region)
                .orElseThrow(() -> new IllegalArgumentException("No such region: " + region));
        multiRegionRepository.setSecondaryRegion(r);

        updateRegions();
    }

    @TransactionImplicit
    public void dropSecondaryRegion() {
        multiRegionRepository.dropSecondaryRegion();

        updateRegions();
    }

    @TransactionImplicit
    public void setSurvivalGaol(SurvivalGoal goal) {
        multiRegionRepository.setSurvivalGoal(goal);
    }

    @TransactionImplicit
    public SurvivalGoal getSurvivalGoal() {
        String goal = multiRegionRepository.getSurvivalGoal().orElse("none");
        return SurvivalGoal.valueOf(goal.toUpperCase());
    }

    @TransactionImplicit
    public String showCreateTable(String table) {
        return multiRegionRepository.showCreateTable(table).orElse("none");
    }

    @TransactionImplicit
    public Optional<Region> getGatewayRegion() {
        Optional<String> gateway = regionRepository.getGatewayRegion();
        if (gateway.isPresent()) {
            return applicationProperties.getRegionWithDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }

    @TransactionImplicit
    public Optional<Region> getPrimaryRegion() {
        Optional<String> gateway = regionRepository.getPrimaryRegion();
        if (gateway.isPresent()) {
            return applicationProperties.getRegionWithDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }

    @TransactionImplicit
    public Optional<Region> getSecondaryRegion() {
        Optional<String> gateway = regionRepository.getSecondaryRegion();
        if (gateway.isPresent()) {
            return applicationProperties.getRegionWithDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }
}
