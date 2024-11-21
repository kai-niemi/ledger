package se.cockroachdb.ledger.domain;

import java.util.EnumSet;

public enum TransferType {
    PAYMENT("Payment"),
    FEE("Fee"),
    REFUND("Refund"),
    CHARGEBACK("Chargeback"),
    GRANT("Grant"),
    BANK("Bank");

    private final String code;

    TransferType(String code) {
        this.code = code;
    }

    public static TransferType of(String code) {
        for (TransferType transferType : EnumSet.allOf(TransferType.class)) {
            if (transferType.code.equals(code)) {
                return transferType;
            }
        }
        throw new IllegalArgumentException("No such transfer type: " + code);
    }

    public String getCode() {
        return code;
    }
}
