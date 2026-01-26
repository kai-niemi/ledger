package io.cockroachdb.ledger.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class RateLimiterTest {
    @Test
    public void testRateLimit() {
        AtomicInteger passed = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();

        Instant then = Instant.now().plus(Duration.ofSeconds(5));

        RateLimiter rateLimiter = new RateLimiter(4);

        while (Instant.now().isBefore(then)) {
            if (rateLimiter.tryAcquire()) {
                passed.incrementAndGet();
            } else {
                lost.incrementAndGet();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("passed: " + passed.get());
        System.out.println("lost: " + lost.get());
    }
}
