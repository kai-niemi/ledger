package io.cockroachdb.ledger.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Region implements Comparable<Region> {
    public static Set<City> joinCities(Collection<Region> regions) {
        return regions.stream()
                .flatMap(region -> region.getCities().stream())
                .collect(Collectors.toSet());
    }

    @NotNull
    private String name;

    private String country;

    private String currency;

    @NotEmpty
    private Set<City> cities = new HashSet<>();

    private final List<String> databaseRegions = new LinkedList<>();

    private boolean primary;

    private boolean secondary;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isSecondary() {
        return secondary;
    }

    public void setSecondary(boolean secondary) {
        this.secondary = secondary;
    }

    public void clearDatabaseRegions() {
        this.databaseRegions.clear();
    }

    public List<String> getDatabaseRegions() {
        return Collections.unmodifiableList(databaseRegions);
    }

    public String getDatabaseRegionSingleton() {
        if (databaseRegions.size() != 1) {
            throw new IllegalStateException(
                    "Expected singleton database region but got %d: %s - check region mappings"
                            .formatted(databaseRegions.size(),
                                    String.join(",", databaseRegions)));
        }
        return databaseRegions.get(0);
    }

    public void addDatabaseRegion(String databaseRegion) {
        this.databaseRegions.add(databaseRegion);
    }

    public String getName() {
        return name;
    }

    public Region setName(String name) {
        this.name = name;
        return this;
    }

    public Set<City> getCities() {
        return Collections.unmodifiableSet(cities);
    }

    public Set<String> getCityNames() {
        return cities.stream().map(City::getName).collect(Collectors.toSet());
    }

    public void setCities(Set<City> cities) {
        this.cities = cities;
    }

    @Override
    public int compareTo(Region o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return "Region{" +
               "name='" + name + '\'' +
               ", cities=" + cities +
               ", databaseRegion='" + databaseRegions + '\'' +
               ", primary=" + primary +
               ", secondary=" + secondary +
               '}';
    }
}

