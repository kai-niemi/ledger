package io.cockroachdb.ledger.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import io.cockroachdb.ledger.ProfileNames;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DataSourceConfig {
    public static final String SQL_TRACE_LOGGER = "io.cockroachdb.ledger.SQL_TRACE";

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSource primaryDataSource() {
        LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();
        proxy.setTargetDataSource(loggingProxy(targetDataSource()));
        proxy.setDefaultAutoCommit(true);
        return proxy;
    }

    private DataSource loggingProxy(DataSource dataSource) {
        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator();
        creator.setMultiline(true);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger(SQL_TRACE_LOGGER);
        listener.setLogLevel(SLF4JLogLevel.TRACE);
        listener.setQueryLogEntryCreator(creator);
        listener.setWriteConnectionId(true);
        listener.setWriteIsolation(true);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .asJson()
                .listener(listener)
                .multiline()
                .build();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource() {
        HikariDataSource ds = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setAutoCommit(true);

        // https://stackoverflow.com/questions/851758/java-enums-jpa-and-postgres-enums-how-do-i-make-them-work-together/43125099#43125099
        if (ProfileNames.acceptsPostgresSQL(environment)) {
            ds.addDataSourceProperty("stringtype", "unspecified");
        }
        return ds;
    }
}
