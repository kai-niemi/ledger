package io.cockroachdb.ledger.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.RateLimiter;

public class MiscTest {
    @Test
    public void testRateLimits() {
        AtomicInteger passed = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();

        Instant then = Instant.now().plus(Duration.ofSeconds(10));

        RateLimiter rateLimiter = RateLimiter.create(.25);

        while (Instant.now().isBefore(then)) {
            if (rateLimiter.tryAcquire()) {
                passed.incrementAndGet();
            } else {
                lost.incrementAndGet();
            }
        }

        System.out.println("passed: " + passed.get());
        System.out.println("lost: " + lost.get());
    }
}
