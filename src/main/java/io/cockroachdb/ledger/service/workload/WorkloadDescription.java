package io.cockroachdb.ledger.service.workload;

@FunctionalInterface
public interface WorkloadDescription {
    String displayValue();

    default String categoryValue() {
        return "uncategorized";
    };
}
