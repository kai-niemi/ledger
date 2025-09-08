package io.cockroachdb.ledger.repository;

import io.cockroachdb.ledger.domain.AccountSummary;
import io.cockroachdb.ledger.domain.TransferSummary;
import io.cockroachdb.ledger.model.City;

public interface ReportingRepository {
    AccountSummary accountSummary(City city);

    TransferSummary transferSummary(City city);
}
