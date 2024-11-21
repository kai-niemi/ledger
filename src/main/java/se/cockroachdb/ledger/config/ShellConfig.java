package se.cockroachdb.ledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import se.cockroachdb.ledger.shell.support.RegionProvider;

@Configuration
public class ShellConfig {
    @Bean
    public RegionProvider regionProvider() {
        return new RegionProvider();
    }
}
