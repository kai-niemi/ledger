package io.cockroachdb.ledger.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.cockroachdb.ledger.util.metrics.TimeSeries;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class InstrumentationConfig {
    @Bean
    public TimeSeries connectionPoolTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, "connection-pool", () -> List.of(
                registry.find("hikaricp.connections.active").tag("pool", "ledger-pool"),
                registry.find("hikaricp.connections.idle").tag("pool", "ledger-pool"),
                registry.find("hikaricp.connections.min").tag("pool", "ledger-pool"),
                registry.find("hikaricp.connections.max").tag("pool", "ledger-pool")
        ));
    }

    @Bean
    public TimeSeries threadPoolTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, "threads", () -> List.of(
                registry.find("jvm.threads.live"),
                registry.find("jvm.threads.peak")
        ));
    }

    @Bean
    public TimeSeries cpuTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries(registry, "system", () -> List.of(
                registry.find("process.cpu.usage"),
                registry.find("system.cpu.usage")
        )
        );
    }
}
