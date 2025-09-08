package io.cockroachdb.ledger.web.model;

import io.cockroachdb.ledger.domain.TransferType;

public class TransferFilterForm {
    private TransferType transferType;

    public TransferFilterForm(TransferType transferType) {
        this.transferType = transferType;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }

    @Override
    public String toString() {
        return "TransferFilterForm{" +
               "transferType=" + transferType +
               '}';
    }
}
