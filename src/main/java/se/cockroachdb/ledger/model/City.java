package se.cockroachdb.ledger.model;

import java.util.Collection;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class City implements Comparable<City> {
    public static City of(String name, String country, String currency) {
        City c = new City();
        c.setName(name);
        c.setCountry(country);
        c.setCurrency(currency);
        return c;
    }

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
     * ISO-3166-1 A3 country codes (for flags)
     */
    @NotNull
    private String country;

    /**
     * ISO-4217 currency code
     */
    @NotNull
    private String currency;

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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        City city = (City) o;
        return Objects.equals(name, city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
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
