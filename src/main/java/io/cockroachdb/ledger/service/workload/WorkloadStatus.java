package io.cockroachdb.ledger.service.workload;

public enum WorkloadStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
