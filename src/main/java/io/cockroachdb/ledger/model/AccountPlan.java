package io.cockroachdb.ledger.model;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

import io.cockroachdb.ledger.util.Multiplier;

@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountPlan {
    private String accountsPerCity;

    @NotNull
    private Double initialBalance;

    public String getAccountsPerCity() {
        return accountsPerCity;
    }

    public int getAccountsPerCityNum() {
        return Multiplier.parseInt(accountsPerCity);
    }

    public void setAccountsPerCity(String accountsPerCity) {
        this.accountsPerCity = accountsPerCity;
    }

    public @NotNull Double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(@NotNull Double initialBalance) {
        this.initialBalance = initialBalance;
    }

    @Override
    public String toString() {
        return "AccountPlan{" +
               "accountsPerCity=" + accountsPerCity +
               ", initialBalance='" + initialBalance + '\'' +
               '}';
    }
}
