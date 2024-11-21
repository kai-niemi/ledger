/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package se.cockroachdb.ledger.util.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

public class TimeSeries {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<DataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    private final List<Meter.Id> meterIds = new ArrayList<>();

    private Duration samplePeriod = Duration.ofSeconds(300);

    private final MeterRegistry meterRegistry;

    private final Supplier<List<Search>> meterSupplier;

    private boolean meterSupplierConsumed;

    private final String name;

    public TimeSeries(MeterRegistry meterRegistry,
                      String name,
                      Supplier<List<Search>> meterSupplier) {
        this.meterRegistry = meterRegistry;
        this.name = name;
        this.meterSupplier = meterSupplier;
    }

    public void registerMeters() {
        meterSupplier.get().forEach(this::addMeter);
    }

    private void addMeter(@NotNull Search search) {
        Meter meter = search.meter();
        if (meter != null) {
            meterIds.add(meter.getId());
        } else {
            logger.warn("Meter not found - check config for time series '{}'", name);
        }
    }

    public void setSamplePeriod(Duration samplePeriod) {
        this.samplePeriod = samplePeriod;
    }

    public void takeSnapshot() {
        if (!meterSupplierConsumed) {
            registerMeters();
            meterSupplierConsumed = true;
        }

        // Purge old data points older than sample period
        dataPoints.removeIf(dataPoint -> dataPoint.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all defined metrics
        DataPoint dataPoint = new DataPoint(Instant.now());

        meterRegistry.getMeters()
                .stream()
                .filter(meter -> {
                    final Meter.Id id = meter.getId();
                    return meterIds.stream().anyMatch(x -> x.equals(id));
                })
                .forEach(meter -> {
                    final Meter.Id id = meter.getId();
                    meter.measure()
                            .forEach(measurement -> {
                                dataPoint.putValue(id.getName(), measurement.getValue());
                            });
                });

        dataPoints.add(dataPoint);
    }

    public List<Map<String, Object>> getDataPoints() {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            List<Long> labels =
                    dataPoints.stream()
                            .map(DataPoint::getInstant)
                            .toList()
                            .stream()
                            .map(Instant::toEpochMilli)
                            .toList();

            Map<String, Object> headerElement = new HashMap<>();
            headerElement.put("data", labels.toArray());

            columnData.add(headerElement);
        }

        meterRegistry
                .getMeters()
                .stream()
                .filter(meter -> {
                    final Meter.Id id = meter.getId();
                    return meterIds.stream().anyMatch(pair -> pair.equals(id));
                })
                .forEach(meter -> {
                    final Meter.Id id = meter.getId();

                    List<Double> data = dataPoints
                            .stream()
                            .filter(dataPoint -> !dataPoint.isExpired())
                            .map(dataPoint -> dataPoint.getValue(id.getName()))
                            .toList();

                    Map<String, Object> dataElement = new HashMap<>();
                    dataElement.put("data", data.toArray());
                    dataElement.put("id", id.getName());
                    dataElement.put("name", "%s".formatted(id.getDescription()));

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
