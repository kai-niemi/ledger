package se.cockroachdb.ledger.domain;

import java.util.Currency;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBatchRequest {
    @NotNull
    private String city;

    @NotNull
    private Currency currency;

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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
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
