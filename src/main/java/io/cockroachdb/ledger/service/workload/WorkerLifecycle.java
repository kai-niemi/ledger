package io.cockroachdb.ledger.service.workload;

import java.time.Duration;

/**
 * Worker lifecycle event handler.
 */
public interface WorkerLifecycle {
    /**
     * Invoked after a successful worker call.
     */
    void callSuccess(Duration callTime);

    /**
     * Invoked after a failed worker call.
     */
    void callFailure(Duration callTime, Exception ex);

    void interrupted(Duration callTime, Exception ex);

    void completed(Duration callTime);
}
