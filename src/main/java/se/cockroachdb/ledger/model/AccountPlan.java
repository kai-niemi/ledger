package se.cockroachdb.ledger.model;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountPlan {
    private int accountsPerCity;

    @NotNull
    private Double initialBalance;

    public int getAccountsPerCity() {
        return accountsPerCity;
    }

    public void setAccountsPerCity(int accountsPerCity) {
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
