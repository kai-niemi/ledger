package se.cockroachdb.ledger.model;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.validation.constraints.NotNull;

import se.cockroachdb.ledger.util.Money;

@JsonPropertyOrder({"links", "embedded"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CityModel extends RepresentationModel<CityModel> {
    @NotNull
    private String name;

    @NotNull
    private String countryCode;

    @NotNull
    private LocalDateTime updatedAt;

    private long numberOfAccounts;

    @NotNull
    private Money totalBalance;

    @NotNull
    private Money minBalance;

    @NotNull
    private Money maxBalance;

    private long numberOfTransfers;

    private long numberOfLegs;

    @NotNull
    private Money totalTurnover;

    public String getLastActive() {
        return updatedAt != null
                ? Duration.between(updatedAt, LocalDateTime.now()).toSeconds() + " seconds" : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @NotNull String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(@NotNull String countryCode) {
        this.countryCode = countryCode;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @NotNull Money getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(@NotNull Money maxBalance) {
        this.maxBalance = maxBalance;
    }

    public @NotNull Money getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(@NotNull Money minBalance) {
        this.minBalance = minBalance;
    }

    public long getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public void setNumberOfAccounts(long numberOfAccounts) {
        this.numberOfAccounts = numberOfAccounts;
    }

    public long getNumberOfLegs() {
        return numberOfLegs;
    }

    public void setNumberOfLegs(long numberOfLegs) {
        this.numberOfLegs = numberOfLegs;
    }

    public long getNumberOfTransfers() {
        return numberOfTransfers;
    }

    public void setNumberOfTransfers(long numberOfTransfers) {
        this.numberOfTransfers = numberOfTransfers;
    }

    public @NotNull Money getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(@NotNull Money totalBalance) {
        this.totalBalance = totalBalance;
    }

    public @NotNull Money getTotalTurnover() {
        return totalTurnover;
    }

    public void setTotalTurnover(@NotNull Money totalTurnover) {
        this.totalTurnover = totalTurnover;
    }
}
