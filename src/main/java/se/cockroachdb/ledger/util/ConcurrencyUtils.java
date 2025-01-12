package se.cockroachdb.ledger.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * Concurrency utility using virtual threads for submitting workloads
 * with a collective timeout and graceful cancellation.
 *
 * @author Kai Niemi
 */
public abstract class ConcurrencyUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyUtils.class);

    /**
     * Permit any number of concurrent invocations: that is, don't throttle concurrency.
     * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
     */
    public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

    private ConcurrencyUtils() {
    }

    public static <V> void runConcurrentlyAndWait(List<Callable<V>> tasks,
                                                  long timeout, TimeUnit timeUnit,
                                                  int concurrencyLimit,
                                                  Consumer<V> consumer) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be > 0");
        }

        final SimpleAsyncTaskScheduler cancellation = new SimpleAsyncTaskScheduler();
        cancellation.setConcurrencyLimit(ForkJoinPool.getCommonPoolParallelism());
        cancellation.setVirtualThreads(true);

        final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(concurrencyLimit);
        executor.setVirtualThreads(true);

        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();

        long expirationTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);

        tasks.forEach(callable -> {
            CompletableFuture<Boolean> f = CompletableFuture.supplyAsync(() -> {
                if (System.currentTimeMillis() > expirationTime) {
                    logger.warn("Task scheduled after expiration time: " + callable);
                    return false;
                }
                Future<V> future = executor.submit(callable);
                long delay = Math.abs(expirationTime - System.currentTimeMillis());
                cancellation.schedule(() -> future.cancel(true), Instant.now().plusMillis(delay));

                try {
                    V result = future.get();
                    consumer.accept(result);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Task interrupt");
                } catch (CancellationException e) {
                    logger.warn("Task cancellation");
                } catch (ExecutionException e) {
                    logger.error("Task execution failed", e.getCause());
                }
                return false;
            });
            allFutures.add(f);
        });

        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[] {})).join();
        } finally {
            executor.close();
            cancellation.close();
        }
    }

    public static <V> int runConcurrentlyAndWait(List<Callable<V>> tasks,
                                                 int concurrencyLimit,
                                                 Consumer<V> consumer) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(concurrencyLimit);
        executor.setVirtualThreads(true);

        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();

        AtomicInteger completions = new AtomicInteger();

        tasks.forEach(callable -> {
            CompletableFuture<Boolean> f = CompletableFuture.supplyAsync(() -> {
                Future<V> future = executor.submit(callable);
                try {
                    V result = future.get();
                    consumer.accept(result);
                    completions.incrementAndGet();
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Task interrupt");
                } catch (CancellationException e) {
                    logger.warn("Task cancellation");
                } catch (ExecutionException e) {
                    logger.error("Task execution failed", e.getCause());
                }
                return false;
            });
            allFutures.add(f);
        });

        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[] {})).join();
        } finally {
            executor.close();
        }

        return completions.get();
    }
}
