package se.cockroachdb.ledger.model;

import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import se.cockroachdb.ledger.util.Money;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountSummary {
    public static AccountSummary empty(City city) {
        AccountSummary summary = new AccountSummary();
        summary.setCity(city.getName());
        summary.setNumberOfAccounts(0);
        summary.setTotalBalance(Money.zero(city.getCurrencyInstance()));
        summary.setMinBalance(Money.zero(city.getCurrencyInstance()));
        summary.setMaxBalance(Money.zero(city.getCurrencyInstance()));
        return summary;
    }

    private String city;

    private long numberOfAccounts;

    private Money totalBalance;

    private Money minBalance;

    private Money maxBalance;

    private LocalDateTime updatedAt;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getLastActive() {
        return updatedAt != null
                ? Duration.between(updatedAt, LocalDateTime.now()).toSeconds() + " seconds" : "";
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public void setNumberOfAccounts(long numberOfAccounts) {
        this.numberOfAccounts = numberOfAccounts;
    }

    public Money getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(Money totalBalance) {
        this.totalBalance = totalBalance;
    }

    public Money getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(Money minBalance) {
        this.minBalance = minBalance;
    }

    public Money getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(Money maxBalance) {
        this.maxBalance = maxBalance;
    }

    @Override
    public String toString() {
        return "AccountSummary{" +
               "region=" + city +
               ", numberOfAccounts=" + numberOfAccounts +
               ", totalBalance=" + totalBalance +
               ", minBalance=" + minBalance +
               ", maxBalance=" + maxBalance +
               '}';
    }
}
