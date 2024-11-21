package se.cockroachdb.ledger.workload;

import java.time.Duration;

/**
 * Worker lifecycle event handler.
 */
public interface WorkerLifecycle {
    /**
     * Invoked after a successful worker call.
     */
    void success(Duration callTime);

    /**
     * Invoked after a failed worker call.
     */
    boolean failure(Duration callTime, Exception ex);
}
