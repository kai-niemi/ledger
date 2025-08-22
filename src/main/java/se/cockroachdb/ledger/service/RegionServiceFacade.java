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

import se.cockroachdb.ledger.annotations.ServiceFacade;
import se.cockroachdb.ledger.annotations.TransactionImplicit;
import se.cockroachdb.ledger.model.ApplicationProperties;
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
        validateRegionMappings();
        updateRegionMappings();
    }

    private void validateRegionMappings() {
        final List<String> errors = new ArrayList<>();

        final List<Region> regions = applicationProperties.getRegions();
        if (regions.isEmpty()) {
            errors.add("No regions found");
        } else {
            logger.info("Found %d app regions".formatted(regions.size()));
        }

        final Map<String, String> regionMappings = applicationProperties.getRegionMappings();
        if (regionMappings.isEmpty()) {
            logger.info("No region mappings found");
        } else {
            logger.info("Found %d region mappings".formatted(regionMappings.size()));
        }

        applicationProperties.getVisibleRegions().forEach(region-> {
            if (regions.stream()
                    .filter(x -> x.getName().equals(region))
                    .findFirst()
                    .isEmpty()) {
                errors.add("Bad visible region filter - app region '%s' does not exist".formatted(region));
            }
        });

        regionMappings.forEach((k, v) -> {
            if (regions.stream()
                    .filter(region -> region.getName().equals(v))
                    .findFirst()
                    .isEmpty()) {
                errors.add("Bad region mapping '%s ->> %s' - app region '%s' does not exist".formatted(k, v, v));
            }
        });

        // Ensure all region cities have a country and currency code
        regions.forEach(region -> {
            region.getCities().forEach(city -> {
                if (!StringUtils.hasLength(city.getCountry())) {
                    if (!StringUtils.hasLength(region.getCountry())) {
                        errors.add("City '%s' in region '%s' missing country code".formatted(city.getName(),
                                region.getName()));
                    } else {
                        city.setCountry(region.getCountry());
                    }
                }

                if (!StringUtils.hasLength(city.getCurrency())) {
                    if (!StringUtils.hasLength(region.getCurrency())) {
                        errors.add("City '%s' in region '%s' missing currency code".formatted(city.getName(),
                                        region.getName()));
                    } else {
                        city.setCurrency(region.getCurrency());
                    }
                }

                try {
                    Currency.getInstance(city.getCurrency());
                } catch (IllegalArgumentException e) {
                    errors.add("City '%s' currency code '%s' is invalid".formatted(city.getName(), city.getCurrency()));
                }
            });
        });

        final List<String> clusterRegionNames = regionRepository.listClusterRegions();
        if (clusterRegionNames.isEmpty()) {
            logger.info("No cluster regions found!");
        } else {
            logger.info("Found %d cluster regions: %s"
                    .formatted(clusterRegionNames.size(), String.join(", ", clusterRegionNames)));
        }

        clusterRegionNames.forEach(name -> {
            if (regions.stream()
                        .filter(region -> region.getName().equals(name))
                        .findFirst()
                        .isEmpty() && !regionMappings.containsKey(name)) {
                errors.add("Database region '%s' not found in region list or mapping!".formatted(name));
            }
        });

        if (!errors.isEmpty()) {
            errors.forEach(logger::error);
            throw new ApplicationContextException("There are configuration errors - check the config/application-<profile>.yml");
        }
    }

    private void updateRegionMappings() {
        final List<String> clusterRegionNames = regionRepository.listClusterRegions();
        final Map<String, String> regionMappings = applicationProperties.getRegionMappings();
        final List<Region> regions = applicationProperties.getRegions();

        // Map regions to actual cluster/database regions
        regions.forEach(region -> {
            region.clearDatabaseRegions();

            // Check if app region matches a current cluster region
            if (clusterRegionNames.contains(region.getName())) {
                region.addDatabaseRegion(region.getName());
            } else {
                // If not, check if there's a mapping.
                // key is cluster region, value is app region.
                regionMappings
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(region.getName()))
                        .forEach(e -> {
                            if (clusterRegionNames.contains(e.getKey())) {
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
            return applicationProperties.findRegionByName(region)
                    .map(List::of).orElseGet(List::of);
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
                        .findRegionByDatabaseRegion(singleton.get());
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
                                .map(Region::getDatabaseRegionSingleton)
                                .collect(Collectors.toSet()))
                )
        );
        multiRegionRepository.addDatabaseRegions(applicationProperties.getRegions());

        logger.info("Adding regional-by-row localities to tables: %s".formatted(String.join(",", RBR_TABLES)));
        RBR_TABLES.forEach(
                table -> multiRegionRepository.setRegionalByRowTable(applicationProperties.getRegions(), table));

        logger.info("Setting survival goal: %s".formatted(goal));
        multiRegionRepository.setSurvivalGoal(goal);

        updateRegionMappings();
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

        updateRegionMappings();
    }

    @TransactionImplicit
    public void addDatabaseRegions() {
        List<Region> regions = applicationProperties.getRegions();

        multiRegionRepository.addDatabaseRegions(regions);

        updateRegionMappings();
    }

    @TransactionImplicit
    public void dropDatabaseRegions() {
        List<Region> regions = applicationProperties.getRegions();

        multiRegionRepository.dropDatabaseRegions(regions);

        updateRegionMappings();
    }

    @TransactionImplicit
    public void setPrimaryRegion(String region) {
        Region r = applicationProperties.findRegionByName(region)
                .orElseThrow(() -> new IllegalArgumentException("No such region: " + region));
        multiRegionRepository.setPrimaryRegion(r);

        updateRegionMappings();
    }

    @TransactionImplicit
    public void setSecondaryRegion(String region) {
        Region r = applicationProperties.findRegionByName(region)
                .orElseThrow(() -> new IllegalArgumentException("No such region: " + region));
        multiRegionRepository.setSecondaryRegion(r);

        updateRegionMappings();
    }

    @TransactionImplicit
    public void dropSecondaryRegion() {
        multiRegionRepository.dropSecondaryRegion();

        updateRegionMappings();
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
            return applicationProperties.findRegionByDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }

    @TransactionImplicit
    public Optional<Region> getPrimaryRegion() {
        Optional<String> gateway = regionRepository.getPrimaryRegion();
        if (gateway.isPresent()) {
            return applicationProperties.findRegionByDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }

    @TransactionImplicit
    public Optional<Region> getSecondaryRegion() {
        Optional<String> gateway = regionRepository.getSecondaryRegion();
        if (gateway.isPresent()) {
            return applicationProperties.findRegionByDatabaseRegion(gateway.get());
        } else {
            return Optional.empty();
        }
    }
}
