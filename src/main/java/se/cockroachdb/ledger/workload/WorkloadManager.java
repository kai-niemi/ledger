package se.cockroachdb.ledger.workload;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;

import se.cockroachdb.ledger.event.WorkloadUpdatedEvent;
import se.cockroachdb.ledger.util.metrics.Metrics;

/**
 * Manager for background workloads and time series data points for call metrics.
 */
@Component
public class WorkloadManager implements DisposableBean {
    private static void backoffDelayWithJitter(AtomicInteger inc) {
        try {
            TimeUnit.MILLISECONDS.sleep(
                    Math.min((long) (Math.pow(2, inc.incrementAndGet()) + Math.random() * 1000), 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final ExceptionClassifier exceptionClassifier = new ExceptionClassifier() {
    };

    private static final AtomicInteger monotonicId = new AtomicInteger();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<Workload> workloads = Collections.synchronizedList(new LinkedList<>());

    private final List<DataPoint<Integer>> dataPoints = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    @Qualifier("workloadTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${application.sample-period-seconds}")
    private int samplePeriodSeconds;

    @Override
    public void destroy() {
//        workloads.stream()
//                .filter(Workload::isRunning)
//                .forEach(Workload::cancel);
    }

    public <T> Workload submitWorker(Worker<T> worker, WorkloadDescription description) {
        final Metrics metrics = Metrics.empty();

        final LinkedList<Throwable> errors = new LinkedList<>();

        final Future<T> future = submit(worker, new WorkerLifecycle() {
            private final AtomicInteger retries = new AtomicInteger();

            @Override
            public void success(Duration callTime) {
                metrics.markSuccess(callTime);
            }

            @Override
            public boolean failure(Duration callTime, Exception ex) {
                if (ex instanceof SQLException
                    || ex instanceof DataAccessException
                    || ex instanceof TransactionSystemException) {

                    Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);

                    if (errors.size() > 100) {
                        errors.removeFirst();
                    }
                    errors.add(cause);

                    if (exceptionClassifier.isTransient(cause)) {
                        if (cause instanceof SQLException) {
                            String sqlState = ((SQLException) cause).getSQLState();
                            logger.warn("Transient SQL error [%s]: [%s]".formatted(sqlState, cause));
                            metrics.markFail(callTime, true);
                        } else {
                            logger.warn("Transient data access error", ex);
                            metrics.markFail(callTime, true);
                        }
                        backoffDelayWithJitter(retries);
                        return true;
                    } else {
                        logger.error("Non-transient data access error", ex);
                        metrics.markFail(callTime, false);
                        return true;
                    }
                } else {
                    // Fatal and futile
                    throw new UndeclaredThrowableException(ex);
                }
            }
        });

        Workload workload = new Workload(monotonicId.incrementAndGet(), future, description, metrics, errors);

        asyncTaskExecutor.submit(() -> {
            try {
                logger.info("Started %s [%s]"
                        .formatted(description.displayValue(), description.categoryValue()));
                workload.awaitCompletion();
            } catch (ExecutionException e) {
                logger.warn(e.getCause().toString());
            } finally {
                workload.setStopTime(Instant.now());
                logger.info("Finished %s [%s]"
                        .formatted(description.displayValue(), description.categoryValue()));
            }
        });

        workloads.add(workload);

        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));

        return workload;
    }

    private <T> Future<T> submit(Worker<T> task, WorkerLifecycle lifecycle) {
        return asyncTaskExecutor.submit(() -> {
            int calls = 1;

            while (task.test(calls++)) {
                if (Thread.interrupted()) {
                    logger.warn("Thread interrupted - bailing out");
                    break;
                }

                final Instant callTime = Instant.now();

                try {
                    task.call();
                    lifecycle.success(Duration.between(callTime, Instant.now()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Thread interrupted - bailing out");
                    break;
                } catch (Exception e) {
                    if (!lifecycle.failure(Duration.between(callTime, Instant.now()), e)) {
                        logger.warn("Unrecoverable exception - bailing out");
                        break;
                    }
                }
            }

            return null;
        });
    }

    public Workload getWorkloadById(Integer id) {
        return workloads
                .stream()
                .filter(workload -> Objects.equals(workload.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No workload with id: " + id));
    }

    public Page<Workload> getWorkloads(Pageable pageable, Predicate<Workload> predicate) {
        List<Workload> content = new ArrayList<>(workloads.stream()
                .filter(predicate)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList());
        int total = workloads.size();
        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    public void deleteWorkload(Integer id) {
        Workload workload = getWorkloadById(id);
        if (workload.isRunning()) {
            throw new IllegalStateException("Workload is running: " + id);
        }

        if (workloads.remove(workload)) {
            applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
        }
    }

    public void deleteWorkloads() {
        cancelWorkloads();
        workloads.clear();
        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
    }

    public void cancelWorkload(Integer id) {
        getWorkloadById(id).cancel();
        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
    }

    public void cancelWorkloads() {
        workloads.stream()
                .filter(Workload::isRunning)
                .forEach(Workload::cancel);
        applicationEventPublisher.publishEvent(new WorkloadUpdatedEvent(this));
    }

    // Time series functions

    public Metrics getMetricsAggregate(Pageable page) {
        List<Metrics> metrics = getWorkloads(page, workloadModel -> true)
                .stream()
                .map(Workload::getMetrics).toList();
        return Metrics.builder()
                .withUpdateTime(Instant.now())
                .withMeanTimeMillis(metrics.stream()
                        .mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withOps(metrics.stream().mapToDouble(Metrics::getOpsPerSec).sum(),
                        metrics.stream().mapToDouble(Metrics::getOpsPerMin).sum())
                .withP50(metrics.stream().mapToDouble(Metrics::getP50).average().orElse(0))
                .withP90(metrics.stream().mapToDouble(Metrics::getP90).average().orElse(0))
                .withP95(metrics.stream().mapToDouble(Metrics::getP95).average().orElse(0))
                .withP99(metrics.stream().mapToDouble(Metrics::getP99).average().orElse(0))
                .withP999(metrics.stream().mapToDouble(Metrics::getP999).average().orElse(0))
                .withMeanTimeMillis(metrics.stream().mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withSuccessful(metrics.stream().mapToInt(Metrics::getSuccess).sum())
                .withFails(metrics.stream().mapToInt(Metrics::getTransientFail).sum(),
                        metrics.stream().mapToInt(Metrics::getNonTransientFail).sum())
                .build();
    }

    public void takeSnapshot() {
        Duration samplePeriod = Duration.ofSeconds(samplePeriodSeconds);

        // Purge old data points older than sample period
        dataPoints.removeIf(item -> item.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all workload metrics
        DataPoint<Integer> dataPoint = new DataPoint<>(Instant.now());

        // Add datapoint if still running
        workloads.stream()
                .filter(Workload::isRunning)
                .forEach(workload -> dataPoint.mark(workload.getId(), workload.getMetrics()));

        dataPoints.add(dataPoint);
    }

    public void clearDataPoints() {
        dataPoints.clear();
    }

    public List<Map<String, Object>> getDataPoints(Function<Metrics, Double> mapper, Pageable page) {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            final Map<String, Object> headerElement = new HashMap<>();
            List<Long> labels =
                    dataPoints.stream()
                            .map(DataPoint::getInstant)
                            .toList()
                            .stream()
                            .map(Instant::toEpochMilli)
                            .toList();
            headerElement.put("data", labels.toArray());
            columnData.add(headerElement);
        }

        getWorkloads(page, (x) -> true)
                .forEach(workload -> {
                    Map<String, Object> dataElement = new HashMap<>();

                    List<Metrics> metrics = new ArrayList<>();

                    dataPoints.forEach(dataPoint -> metrics.add(dataPoint.get(workload.getId())));

                    List<Double> data = metrics
                            .stream()
                            .filter(metric -> {
                                // TODO!
                                return true;// Instant.now().plusSeconds(10).isBefore(metric.getUpdateTime());
                            })
                            .map(mapper)
                            .toList();

                    dataElement.put("id", workload.getId());
                    dataElement.put("name", "%s (%d)".formatted(workload.getTitle(), workload.getId()));
                    dataElement.put("data", data.toArray());

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
