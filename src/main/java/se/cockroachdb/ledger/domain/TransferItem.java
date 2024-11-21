package se.cockroachdb.ledger.domain;

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

import se.cockroachdb.ledger.util.Money;

/**
 * Immutable item or account leg representing a single account balance update
 * as part of a balanced, multi-legged monetary transaction.
 * <p>
 * Mapped as a join with attributes between the Account and Transfer entities.
 */
@Entity
@Table(name = "transfer_item")
//@DynamicInsert
public class TransferItem extends AbstractEntity<TransferItemId> {
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
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId") // refs the embeddable primary key
    @JoinColumn(name = "account_id", // referencedColumnName = "id",
            nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

    protected TransferItem() {
    }

    public TransferItem(Transfer transfer, Account account, int itemPos) {
        this.id = new TransferItemId(transfer.getId(), account.getId(), itemPos);
        this.transfer = transfer;
        this.account = account;
    }

    @Override
    public TransferItemId getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Transfer getTransfer() {
        return transfer;
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
        private final List<TransferItem> items = new ArrayList<>();

        private Transfer transfer;

        private TransferItem current = new TransferItem();

        public Builder withTransfer(Transfer transfer) {
            this.transfer = transfer;
            return this;
        }

        public Builder withAccount(Account account) {
            current.account = account;
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
            Assert.notNull(transfer, "transfer");
            Assert.notNull(current.account, "account is null");
            Assert.notNull(current.city, "city is null");

            TransferItem transferItem = new TransferItem(transfer, current.account, items.size());
            transferItem.setCity(current.city);
            transferItem.setAmount(current.amount);
            transferItem.setRunningBalance(current.runningBalance);
            transferItem.setNote(current.note);

            items.add(transferItem);

            current = new TransferItem();

            return this;
        }

        public List<TransferItem> build() {
            return items;
        }
    }
}
