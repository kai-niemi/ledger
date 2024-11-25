package se.cockroachdb.ledger.web.api.csv;

import java.io.PrintWriter;
import java.time.LocalDateTime;

import se.cockroachdb.ledger.domain.Account;

public class AccountCsvWriter implements CsvWriter<Account> {
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
    public void writeItem(Account item) {
        String line = String.join(",",
                "" + item.getId(),
                item.getCity(),
                item.getName(),
                "" + item.getBalance().getAmount(),
                "" + item.getBalance().getCurrency(),
                "" + item.getAllowNegative(),
                "" + item.getAccountType().getCode(),
                LocalDateTime.now().toString()
        );
        pw.println(line);
    }

    @Override
    public void writeFooter() {
    }
}
