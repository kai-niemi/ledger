package se.cockroachdb.ledger.domain;

import java.util.EnumSet;

public enum AccountType {
    EXPENSE("Expense"),
    ASSET("Asset"),
    REVENUE("Revenue"),
    LIABILITY("Liability"),
    EQUITY("Equity");

    private final String code;

    AccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccountType of(String code) {
        for (AccountType accountType : EnumSet.allOf(AccountType.class)) {
            if (accountType.code.equals(code)) {
                return accountType;
            }
        }
        throw new IllegalArgumentException("No such account type: " + code);
    }
}
