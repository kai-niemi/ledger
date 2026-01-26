package io.cockroachdb.ledger.util;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple thread-safe token bucket rate limiter.
 *
 * @author Kai Niemi
 */
public class RateLimiter {
    private final int permitsPerSec;

    private final int refillRatePerSec;

    private final AtomicInteger tokens;

    private final AtomicLong lastRefillTimestamp = new AtomicLong(System.nanoTime());

    public RateLimiter(int permitsPerSec) {
        this(permitsPerSec, 10);
    }

    public RateLimiter(int permitsPerSec, int refillRatePerSec) {
        this.permitsPerSec = permitsPerSec;
        this.refillRatePerSec = refillRatePerSec;
        this.tokens = new AtomicInteger(permitsPerSec);
    }

    public boolean tryAcquire() {
        refill();
        return tokens.getAndUpdate(operand -> {
            if (operand > 0) {
                return operand - 1;
            }
            return operand;
        }) > 0;
    }

    private void refill() {
        lastRefillTimestamp.updateAndGet(operand -> {
            long elapsedTime = System.nanoTime() - operand;
            long secondsPassed = Duration.ofNanos(elapsedTime).toSeconds();
            if (secondsPassed > 0) {
                int tokensToAdd = (int) (secondsPassed * refillRatePerSec);
                tokens.updateAndGet(x -> Math.min(permitsPerSec, x + tokensToAdd));
                return operand + (Duration.ofSeconds(secondsPassed).toNanos());
            }
            return operand;
        });
    }
}
