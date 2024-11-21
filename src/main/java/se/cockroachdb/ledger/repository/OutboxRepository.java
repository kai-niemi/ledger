package se.cockroachdb.ledger.repository;

import se.cockroachdb.ledger.annotations.EventAggregate;

public interface OutboxRepository {
    <ID> void writeEvent(EventAggregate<ID> event);

    void deleteAllInBatch();
}
