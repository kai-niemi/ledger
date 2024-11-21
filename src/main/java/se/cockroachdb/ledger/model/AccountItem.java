package se.cockroachdb.ledger.model;

import java.util.UUID;

import se.cockroachdb.ledger.util.Money;

public class AccountItem {
    private UUID id;

    private Money amount;

    private String note;

    private String city;

    public UUID getId() {
        return id;
    }

    public Money getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public String getCity() {
        return city;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
