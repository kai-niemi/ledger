package io.cockroachdb.ledger.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import io.cockroachdb.ledger.util.Money;

/**
 * Immutable item or account leg representing a single account balance update
 * as part of a balanced, multi-legged monetary transaction.
 * <p>
 * Mapped as a join with attributes between the Account and Transfer entities.
 */
@Entity
@Table(name = "transfer_item")
public class TransferItemEntity extends AbstractEntity<TransferItemId> {
    @EmbeddedId
    private TransferItemId id;

    @Column(name = "city")
    private String city;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "amount", nullable = false, updatable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "currency", length = 3, nullable = false,
                            updatable = false))
    })
    private Money amount;

    @Column(name = "note", length = 128, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String note;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "running_balance", nullable = false, updatable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "currency", length = 3, nullable = false, insertable = false,
                            updatable = false))
    })
    private Money runningBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("transferId") // refs the embeddable primary key
    @JoinColumn(name = "transfer_id", // referencedColumnName = "id",
            nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private TransferEntity transferEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId") // refs the embeddable primary key
    @JoinColumn(name = "account_id", // referencedColumnName = "id",
            nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private AccountEntity accountEntity;

    protected TransferItemEntity() {
    }

    public TransferItemEntity(TransferEntity transferEntity, AccountEntity accountEntity, int itemPos) {
        this.id = new TransferItemId(transferEntity.getId(), accountEntity.getId(), itemPos);
        this.transferEntity = transferEntity;
        this.accountEntity = accountEntity;
    }

    @Override
    public TransferItemId getId() {
        return id;
    }

    public AccountEntity getAccountEntity() {
        return accountEntity;
    }

    public TransferEntity getTransferEntity() {
        return transferEntity;
    }

    public String getCity() {
        return city;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Money getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(Money runningBalance) {
        this.runningBalance = runningBalance;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "TransferItem{" +
               ", id=" + id +
               ", city='" + city + '\'' +
               ", amount=" + amount +
               ", note='" + note + '\'' +
               ", runningBalance=" + runningBalance +
               "} " + super.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<TransferItemEntity> items = new ArrayList<>();

        private TransferEntity transferEntity;

        private TransferItemEntity current = new TransferItemEntity();

        public Builder withTransfer(TransferEntity transferEntity) {
            this.transferEntity = transferEntity;
            return this;
        }

        public Builder withAccount(AccountEntity accountEntity) {
            current.accountEntity = accountEntity;
            return this;
        }

        public Builder withAmount(Money amount) {
            current.amount = amount;
            return this;
        }

        public Builder withRunningBalance(Money runningBalance) {
            current.runningBalance = runningBalance;
            return this;
        }

        public Builder withNote(String note) {
            current.note = note;
            return this;
        }

        public Builder withCity(String city) {
            current.city = city;
            return this;
        }

        public Builder and() {
            Assert.notNull(transferEntity, "transfer");
            Assert.notNull(current.accountEntity, "account is null");
            Assert.notNull(current.city, "city is null");

            TransferItemEntity transferItemEntity = new TransferItemEntity(transferEntity, current.accountEntity, items.size());
            transferItemEntity.setCity(current.city);
            transferItemEntity.setAmount(current.amount);
            transferItemEntity.setRunningBalance(current.runningBalance);
            transferItemEntity.setNote(current.note);

            items.add(transferItemEntity);

            current = new TransferItemEntity();

            return this;
        }

        public List<TransferItemEntity> build() {
            return items;
        }
    }
}
