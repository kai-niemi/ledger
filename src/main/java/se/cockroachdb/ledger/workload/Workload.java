package se.cockroachdb.ledger.workload;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import se.cockroachdb.ledger.util.DurationUtils;
import se.cockroachdb.ledger.util.metrics.Metrics;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Workload {
    private final Integer id;

    @JsonIgnore
    private final Future<?> future;

    private final Instant startTime;

    private Instant stopTime;

    private final WorkloadDescription workloadDescription;

    private final Metrics metrics;

    private final LinkedList<Throwable> errors;

    Workload(Integer id,
                    Future<?> future,
                    WorkloadDescription workloadDescription,
                    Metrics metrics,
                    LinkedList<Throwable> errors) {
        this.id = id;
        this.future = future;
        this.workloadDescription = workloadDescription;
        this.metrics = metrics;
        this.errors = errors;
        this.startTime = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStopTime(Instant stopTime) {
        this.stopTime = stopTime;
    }

    public String getTitle() {
        return workloadDescription.displayValue();
    }

    public String getCategory() {
        return workloadDescription.categoryValue();
    }

    public List<Throwable> getLastErrors() {
        return Collections.unmodifiableList(errors);
    }

    public Metrics getMetrics() {
        return isRunning() ? metrics : Metrics.copy(metrics);
    }

    public String getExecutionTime() {
        return DurationUtils.durationToDisplayString(getExecutionDuration());
    }

    public Duration getExecutionDuration() {
        return stopTime != null ? Duration.between(startTime, stopTime)
                : Duration.between(startTime, Instant.now());
    }

    public boolean isRunning() {
        return !future.isDone();
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public boolean cancel() {
        return future.cancel(true);
    }

    public void awaitCompletion() throws ExecutionException {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Workload workload = (Workload) o;
        return Objects.equals(id, workload.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
