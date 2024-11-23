package se.cockroachdb.ledger.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

import se.cockroachdb.ledger.domain.AccountType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBatch {
    @NotNull
    private City city;

    @NotNull
    private String prefix;

    @NotNull
    private Integer batchSize;

    @NotNull
    private AccountType accountType;

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
