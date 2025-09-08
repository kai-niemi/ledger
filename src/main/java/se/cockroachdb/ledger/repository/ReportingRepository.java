package se.cockroachdb.ledger.repository;

import se.cockroachdb.ledger.domain.AccountSummary;
import se.cockroachdb.ledger.domain.TransferSummary;
import se.cockroachdb.ledger.model.City;

public interface ReportingRepository {
    AccountSummary accountSummary(City city);

    TransferSummary transferSummary(City city);
}
