package se.cockroachdb.ledger.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class ThreadTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void startThreads(Executor taskExecutor,
                             CountDownLatch countDownLatch,
                             int numThreads) {
        IntStream.rangeClosed(1, numThreads)
                .forEach(value -> {
                    taskExecutor.execute(() -> {
                        try {
//                            logger.info("%d - completed: %d PoolSize: %d Active: %d".formatted(value,
//                                    taskExecutor.getCompletedTaskCount(),
//                                    taskExecutor.getPoolSize(),
//                                    taskExecutor.getActiveCount()
                            logger.info("%d entering I/O wait".formatted(value));
                            Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 1500));
                            countDownLatch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

                });
    }

    public static ThreadPoolExecutor boundedThreadPool(int numThreads) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads / 2, numThreads,
                0L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(numThreads));
        executor.setRejectedExecutionHandler((runnable, exec) -> {
            try {
                exec.getQueue().put(runnable);
                if (exec.isShutdown()) {
                    throw new RejectedExecutionException(
                            "Task " + runnable + " rejected from " + exec);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("", e);
            }
        });
        return executor;
    }

    public static SimpleAsyncTaskExecutor boundedVirtualThreadPool(int numThreads) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("ledger-test-");
        executor.setConcurrencyLimit(numThreads);
        executor.setVirtualThreads(true);
        return executor;
    }

    @Test
    public void whenCorePoolSizeFiveAndMaxPoolSizeTenAndQueueCapacityTen_thenTenThreads() {
//        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//        taskExecutor.setCorePoolSize(5);
//        taskExecutor.setMaxPoolSize(200);
//        taskExecutor.setQueueCapacity(200);
//        taskExecutor.afterPropertiesSet();

//        Executors.newScheduledThreadPool(3);
        Executor threadPoolExecutor = boundedVirtualThreadPool(500);

        CountDownLatch countDownLatch = new CountDownLatch(128);

        startThreads(threadPoolExecutor, countDownLatch, 128);

        logger.info("All threads completed");
    }
}
