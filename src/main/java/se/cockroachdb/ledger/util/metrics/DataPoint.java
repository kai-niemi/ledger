/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package se.cockroachdb.ledger.util.metrics;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataPoint {
    private final Instant instant;

    private final Map<String, Double> metrics = new LinkedHashMap<>();

    public DataPoint(Instant instant) {
        this.instant = instant;
    }

    public boolean isExpired() {
        return false;
    }

    public Instant getInstant() {
        return instant;
    }

    public void putValue(String id, Double metric) {
        metrics.put(id, metric);
    }

    public Double getValue(String id) {
        return metrics.get(id);
    }
}

