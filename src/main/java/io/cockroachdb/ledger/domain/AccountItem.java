package io.cockroachdb.ledger.domain;

import java.util.UUID;

import io.cockroachdb.ledger.util.Money;

public class AccountItem {
    private UUID id;

    private Money amount;

    private String note;

    public UUID getId() {
        return id;
    }

    public Money getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
