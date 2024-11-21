package se.cockroachdb.ledger.model;

import java.util.Collection;
import java.util.Currency;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class City implements Comparable<City> {
    public static Optional<City> findByName(Collection<City> collection, String name) {
        return collection.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    public static Set<String> joinCityNames(Collection<City> cities) {
        return cities.stream()
                .map(City::getName)
                .collect(Collectors.toSet());
    }

    /**
     * City name in lower case.
     */
    @NotNull
    private String name;

    /**
     * ISO-4217 currency code
     */
    private String currency;

    /**
     * ISO-3166-1 A3 country codes (for flags)
     */
    private String country;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    @JsonIgnore
    public Currency getCurrencyInstance() throws IllegalArgumentException {
        return Currency.getInstance(currency);
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public int compareTo(City o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        City city = (City) o;
        return name.equals(city.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "City{" +
               "country='" + country + '\'' +
               ", name='" + name + '\'' +
               ", currency='" + currency + '\'' +
               '}';
    }
}
