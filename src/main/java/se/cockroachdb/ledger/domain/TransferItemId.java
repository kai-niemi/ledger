package se.cockroachdb.ledger.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite primary key for transfer items / legs pointing back
 * at the transfer and account records.
 */
@Embeddable
public class TransferItemId implements Serializable {
    public static TransferItemId of(UUID transferId, UUID accountId, int itemPos) {
        return new TransferItemId(transferId, accountId, itemPos);
    }

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "transfer_id", updatable = false)
    private UUID transferId;

    @Column(name = "item_pos", updatable = false)
    private Integer itemPos;

    protected TransferItemId() {
    }

    protected TransferItemId(UUID transferId, UUID accountId, int itemPos) {
        Assert.notNull(transferId, "transferId is null");
        Assert.notNull(accountId, "accountId is null");
        this.transferId = transferId;
        this.accountId = accountId;
        this.itemPos= itemPos;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public Integer getItemPos() {
        return itemPos;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferItemId that = (TransferItemId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(transferId, that.transferId)
               && Objects.equals(itemPos, that.itemPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, transferId, itemPos);
    }
}
