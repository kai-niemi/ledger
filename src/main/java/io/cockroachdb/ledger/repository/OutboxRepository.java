package io.cockroachdb.ledger.repository;

import io.cockroachdb.ledger.annotations.EventAggregate;

public interface OutboxRepository {
    <ID> void writeEvent(EventAggregate<ID> event);

    void deleteAllInBatch();
}
