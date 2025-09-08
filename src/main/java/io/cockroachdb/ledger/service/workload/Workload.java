package io.cockroachdb.ledger.service.workload;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.cockroachdb.ledger.util.DurationUtils;
import io.cockroachdb.ledger.util.metrics.Metrics;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Workload {
    private final Integer id;

    @JsonIgnore
    private final Future<?> future;

    private final Instant startTime;

    private Instant stopTime;

    private final WorkloadDescription workloadDescription;

    private final Metrics metrics;

    private final LinkedList<Problem> problems;

    private boolean failed;

    Workload(Integer id,
             Future<?> future,
             WorkloadDescription workloadDescription,
             Metrics metrics,
             LinkedList<Problem> problems) {
        this.id = id;
        this.future = future;
        this.workloadDescription = workloadDescription;
        this.metrics = metrics;
        this.problems = problems;
        this.startTime = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public WorkloadStatus getStatus() {
        if (failed) {
            return WorkloadStatus.FAILED;
        } else if (isRunning()) {
            return WorkloadStatus.RUNNING;
        } else if (isCancelled()) {
            return WorkloadStatus.CANCELLED;
        } else {
            return WorkloadStatus.COMPLETED;
        }
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setCompletion(Instant stopTime, Optional<Problem> failed) {
        this.stopTime = stopTime;
        this.failed = failed.isPresent();
        failed.ifPresent(this.problems::addFirst);
    }

    public Instant getStopTime() {
        return stopTime;
    }

    public String getTitle() {
        return workloadDescription.displayValue();
    }

    public String getCategory() {
        return workloadDescription.categoryValue();
    }

    public List<Problem> getLastProblems() {
        return Collections.unmodifiableList(problems);
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
            setCompletion(Instant.now(), Optional.empty());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            setCompletion(Instant.now(), Optional.of(Problem.from(e)));
            throw new UndeclaredThrowableException(e);
        } catch (ExecutionException e) {
            setCompletion(Instant.now(), Optional.of(Problem.from(e.getCause())));
            throw e;
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
