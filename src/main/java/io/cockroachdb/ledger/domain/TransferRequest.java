package io.cockroachdb.ledger.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.util.Money;

/**
 * A transfer request composed by a list of accounts forming a balanced,
 * multi-legged monetary transaction. That is, the request balance total
 * must equal zero.
 * <p>
 * A request must have at least two items, called transfer legs, a city
 * and a client generated reference ID for idempotency and a transfer type.
 * <p>
 * Each transfer leg points to a single account by id, and includes
 * an amount that is either positive (credit) or negative (debit).
 * <p>
 * It is possible to have legs with different account currencies as long
 * as the total balance for entries with the same currency is zero.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferRequest {
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    private UUID id;

    @NotNull
    private City city;

    @NotBlank
    private TransferType transferType;

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    private LocalDate transferDate;

    @NotEmpty
    private final List<AccountItem> accountItems = new ArrayList<>();

    protected TransferRequest() {
    }

    public UUID getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public List<AccountItem> getAccountItems() {
        return Collections.unmodifiableList(accountItems);
    }

    public static class Builder {
        private final TransferRequest instance = new TransferRequest();

        public Builder withId(UUID id) {
            this.instance.id = id;
            return this;
        }

        public Builder withCity(City city) {
            this.instance.city = city;
            return this;
        }

        public Builder withTransferType(TransferType transactionType) {
            this.instance.transferType = transactionType;
            return this;
        }

        public Builder withBookingDate(LocalDate bookingDate) {
            this.instance.bookingDate = bookingDate;
            return this;
        }

        public Builder withTransferDate(LocalDate transferDate) {
            this.instance.transferDate = transferDate;
            return this;
        }

        public AccountItemBuilder addItem() {
            return new AccountItemBuilder(this, instance.accountItems::add);
        }

        public TransferRequest build() {
            if (instance.accountItems.size() < 2) {
                throw new IllegalStateException("At least two account items are required");
            }
            Assert.notNull(instance.id, "id is required");
            Assert.notNull(instance.transferType, "transferType is required");
            return instance;
        }
    }

    public static class AccountItemBuilder {
        private final AccountItem item = new AccountItem();

        private final Builder parentBuilder;

        private final Consumer<AccountItem> callback;

        private AccountItemBuilder(Builder parentBuilder, Consumer<AccountItem> callback) {
            this.parentBuilder = parentBuilder;
            this.callback = callback;
        }

        public AccountItemBuilder withId(UUID id) {
            this.item.setId(id);
            return this;
        }

        public AccountItemBuilder withAmount(Money amount) {
            this.item.setAmount(amount);
            return this;
        }

        public AccountItemBuilder withNote(String note) {
            this.item.setNote(note);
            return this;
        }

        public Builder then() {
            if (item.getId() == null) {
                throw new IllegalStateException("id is required");
            }
            if (item.getAmount() == null) {
                throw new IllegalStateException("amount is required");
            }
            callback.accept(item);
            return parentBuilder;
        }
    }
}
