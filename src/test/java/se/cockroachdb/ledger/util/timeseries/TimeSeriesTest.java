package se.cockroachdb.ledger.util.timeseries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import se.cockroachdb.ledger.shell.support.JsonHelper;
import se.cockroachdb.ledger.util.metrics.TimeSeries;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("integration-test")
public class TimeSeriesTest {
    @Test
    public void givenStandardMetrics_whenSamplingMeasurements_thenDataPointsAggregated() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);

        final TimeSeries ts = new TimeSeries(registry, "common", () ->
                List.of(registry.find("process.cpu.usage"),
                        registry.find("jvm.threads.started")
                ));
        ts.registerMeters();

        System.out.printf("Sampling");
        IntStream.rangeClosed(1, 10).forEach(value -> {
            ts.takeSnapshot();
            System.out.printf(".");
            System.out.flush();
            sleep1s();
        });
        System.out.println();

        List<Map<String, Object>> dataPoints1 = ts.getDataPoints();
        System.out.println(JsonHelper.toFormattedJSON(dataPoints1));
//        System.out.println();
//        System.out.println(JsonHelper.toFormattedJSON(dataPoints2));
    }

    private void sleep1s() {
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}