package se.cockroachdb.ledger.repository;

import se.cockroachdb.ledger.model.AccountSummary;
import se.cockroachdb.ledger.model.TransferSummary;

import java.util.Optional;

public interface ReportingRepository {
    Optional<AccountSummary> accountSummary(String city);

    Optional<TransferSummary> transactionSummary(String city);
}
