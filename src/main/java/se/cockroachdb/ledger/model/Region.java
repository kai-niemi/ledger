package se.cockroachdb.ledger.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Region implements Comparable<Region> {
    public static List<City> joinCities(Collection<Region> regions) {
        return regions.stream()
                .flatMap(region -> region.getCities().stream())
                .collect(Collectors.toList());
    }

    @NotNull
    private String name;

    private String country;

    private String currency;

    @NotEmpty
    private List<City> cities = new LinkedList<>();

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

    public List<String> getDatabaseRegions() {
        return databaseRegions;
    }

    public String getDatabaseRegion() {
        if (databaseRegions.size() != 1) {
            throw new IllegalStateException("Expected one database region but got " + databaseRegions.size());
        }
        return databaseRegions.get(0);
    }

    public void addDatabaseRegion(String databaseRegion) {
        this.databaseRegions.add(databaseRegion);
    }

    public void clearDatabaseRegions() {
        this.databaseRegions.clear();
    }

    public String getName() {
        return name;
    }

    public Region setName(String name) {
        this.name = name;
        return this;
    }

    public List<City> getCities() {
        return Collections.unmodifiableList(cities);
    }

    public List<String> getCityNames() {
        return cities.stream().map(City::getName).toList();
    }

    public void setCities(List<City> cities) {
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

