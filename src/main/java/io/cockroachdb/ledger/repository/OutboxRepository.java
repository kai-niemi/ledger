package io.cockroachdb.ledger.repository;

import io.cockroachdb.ledger.annotation.EventAggregate;

public interface OutboxRepository {
    <ID> void writeEvent(EventAggregate<ID> event);

    void deleteAllInBatch();
}
