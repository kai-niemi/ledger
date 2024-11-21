package se.cockroachdb.ledger.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionTimedOutException;

import static se.cockroachdb.ledger.util.DurationUtils.executionDuration;

@Disabled("for now")
public class TimeBoundExecutionTest {
    private static final Logger logger = LoggerFactory.getLogger(TimeBoundExecutionTest.class);

    public static Integer doMassiveCompute_AndBlock(int value) {
        logger.debug("Doing massive compute ({}) - will block for eternity", value);
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        throw new IllegalStateException();
    }

    public static Integer doMassiveCompute_AndSucceed(int value, long delayMillis) {
        logger.debug("Doing massive compute ({}) - will succeed after {}", value, delayMillis);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("Done: result of compute({}) is {}", value, value * value);
        return value * value;
    }

    public static Integer doMassiveCompute_AndFail(int value, long delayMillis) {
        logger.debug("Doing massive compute ({}) - will fail after {}", value, delayMillis);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new TransactionTimedOutException("Fail");
    }

    @Test
    public void whenSchedulingManyTasks_withinTime_thenSucceed() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int k = i + 1;
            tasks.add(() -> doMassiveCompute_AndSucceed(k, 1000));
        }

        Duration time = executionDuration(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS,
                    ConcurrencyUtils.UNBOUNDED_CONCURRENCY, x -> {
                    });
            return null;
        });
        Assertions.assertTrue(time.toMillis() <= 15_000, "" + time);
    }

    @Test
    public void whenSchedulingManyTasks_thatTimeout_thenFail() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int k = i + 1;
            tasks.add(() -> doMassiveCompute_AndSucceed(k, 15000));
        }

        Duration time = executionDuration(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, ConcurrencyUtils.UNBOUNDED_CONCURRENCY,
                    result -> {
                        logger.debug("Result ({})", result);
                    });
            return null;
        });
        Assertions.assertTrue(time.toMillis() <= 15_000, "" + time);
    }

    @Test
    public void whenSchedulingManyRandomTasks_withinTime_thenSucceed() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int k = i + 1;
            if (i % 2 == 0) {
                tasks.add(() -> doMassiveCompute_AndFail(k, 5000));
            } else {
                tasks.add(() -> doMassiveCompute_AndSucceed(k, (long) (Math.random() * 5000 + 5000)));
            }
        }

        Duration time = executionDuration(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, ConcurrencyUtils.UNBOUNDED_CONCURRENCY,
                    x -> {
                    });
            return null;
        });
        Assertions.assertTrue(time.toMillis() <= 15_000, "" + time);
    }

    @Test
    public void whenSchedulingManyRandomTasks_thatTimeout_thenFail() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(() -> doMassiveCompute_AndFail(1, 5000));
        tasks.add(() -> doMassiveCompute_AndSucceed(2, (long) (Math.random() * 5000 + 5000)));
        tasks.add(() -> doMassiveCompute_AndBlock(2));
        tasks.add(() -> doMassiveCompute_AndFail(1, 5000));
        tasks.add(() -> doMassiveCompute_AndSucceed(2, (long) (Math.random() * 5000 + 5000)));
        tasks.add(() -> doMassiveCompute_AndBlock(2));
        tasks.add(() -> doMassiveCompute_AndFail(1, 5000));
        tasks.add(() -> doMassiveCompute_AndSucceed(2, (long) (Math.random() * 5000 + 5000)));
        tasks.add(() -> doMassiveCompute_AndBlock(2));

        Duration time = executionDuration(() -> {
            ConcurrencyUtils.runConcurrentlyAndWait(tasks, 10, TimeUnit.SECONDS, ConcurrencyUtils.UNBOUNDED_CONCURRENCY,
                    x -> {
                    });
            return null;
        });
        Assertions.assertTrue(time.toMillis() <= 15_000, "" + time);
    }
}
