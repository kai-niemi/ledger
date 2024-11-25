package se.cockroachdb.ledger.domain;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;

import se.cockroachdb.ledger.util.Money;

/**
 * Represents a monetary account like asset, liability, expense, capital accounts and so forth.
 */
@Entity
@Table(name = "account")
@DynamicInsert
@DynamicUpdate
public class Account extends AbstractEntity<UUID> {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column
    @Basic(fetch = FetchType.LAZY)
    private String description;

    @Convert(converter = AccountTypeConverter.class)
    @Column(name = "type", updatable = false, nullable = false)
    private AccountType accountType;

    @Column(name = "updated_at")
    @Basic(fetch = FetchType.LAZY)
    private LocalDateTime updatedAt;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money balance;

    @Column(nullable = false)
    private boolean closed;

    @Column(nullable = false)
    private int allowNegative;

    protected Account() {
    }

    @PostUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void postLoad() {
        Currency currency = balance.getCurrency();
        this.balance = Money.of(balance.getAmount()
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY), currency);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getBalance() {
        return balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public int getAllowNegative() {
        return allowNegative;
    }

    public boolean isAllowedNegative() {
        return allowNegative > 0;
    }

    @Override
    public String toString() {
        return "Account{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", city='" + city + '\'' +
               ", accountType=" + accountType +
               ", balance=" + balance +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }

        Account that = (Account) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Account instance = new Account();

        public Builder withGeneratedId() {
            withId(UUID.randomUUID());
            return this;
        }

        public Builder withId(UUID accountId) {
            this.instance.id = accountId;
            return this;
        }

        public Builder withName(String name) {
            this.instance.name = name;
            return this;
        }

        public Builder withCity(String city) {
            this.instance.city = city;
            return this;
        }

        public Builder withBalance(Money balance) {
            this.instance.balance = balance;
            return this;
        }

        public Builder withAccountType(AccountType accountType) {
            this.instance.accountType = accountType;
            return this;
        }

        public Builder withClosed(boolean closed) {
            this.instance.closed = closed;
            return this;
        }

        public Builder withAllowNegative(boolean allowNegative) {
            this.instance.allowNegative = allowNegative ? 1 : 0;
            return this;
        }

        public Builder withDescription(String description) {
            this.instance.description = description;
            return this;
        }

        public Builder withUpdated(LocalDateTime updated) {
            this.instance.updatedAt = updated;
            return this;
        }

        public Account build() {
            return instance;
        }
    }
}
