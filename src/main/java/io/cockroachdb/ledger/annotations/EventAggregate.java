package io.cockroachdb.ledger.annotations;

public interface EventAggregate<ID> {
    ID getEventId();

    ID getEntityId();
}
