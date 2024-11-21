package se.cockroachdb.ledger.util;

@FunctionalInterface
public interface WeightedItem {
    double getWeight();
}
