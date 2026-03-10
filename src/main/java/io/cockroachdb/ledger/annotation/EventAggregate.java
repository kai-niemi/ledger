package io.cockroachdb.ledger.annotation;

public interface EventAggregate<ID> {
    ID getEventId();

    ID getEntityId();
}
