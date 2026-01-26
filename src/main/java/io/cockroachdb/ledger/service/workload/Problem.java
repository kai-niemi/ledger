package io.cockroachdb.ledger.service.workload;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public class Problem {
    public static Problem from(String title, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return new Problem(title, t.getClass().getSimpleName(), t.getMessage(), sw.toString());
    }

    private final String title;

    private final String className;

    private final String message;

    private final String stackTrace;

    private final Instant createdAt = Instant.now();

    public Problem(String title, String className, String message, String stackTrace) {
        this.title = title;
        this.className = className;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
