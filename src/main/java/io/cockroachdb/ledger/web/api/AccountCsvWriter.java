package io.cockroachdb.ledger.web.api;

import java.io.PrintWriter;
import java.time.LocalDateTime;

import io.cockroachdb.ledger.domain.AccountEntity;

public class AccountCsvWriter implements CsvWriter<AccountEntity> {
    private final PrintWriter pw;

    public AccountCsvWriter(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void writeHeader() {
        pw.println(String.join(",",
                "id",
                "city",
                "name",
                "balance",
                "currency",
                "allow_negative",
                "account_type",
                "updated_at"
        ));
    }

    @Override
    public void writeItem(AccountEntity item) {
        String line = String.join(",",
                "" + item.getId(),
                item.getCity(),
                item.getName(),
                "" + item.getBalance().getAmount(),
                "" + item.getBalance().getCurrency(),
                "" + item.getAllowNegative(),
                item.getAccountType().getCode(),
                LocalDateTime.now().toString()
        );
        pw.println(line);
    }

    @Override
    public void writeFooter() {
    }
}
