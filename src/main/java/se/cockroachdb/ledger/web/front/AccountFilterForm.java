package se.cockroachdb.ledger.web.front;

import se.cockroachdb.ledger.domain.AccountType;

public class AccountFilterForm {
    private AccountType accountType;

    public AccountFilterForm(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
