package io.cockroachdb.ledger.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.JsonHelper;

@Component
public class DatabasePoolCommands extends AbstractShellCommand {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private ObjectMapper objectMapper;

    private ConnectionPoolState getConnectionPoolSize() {
        return from(hikariDataSource.getHikariPoolMXBean());
    }

    private ConnectionPoolConfig getConnectionPoolConfig() {
        return from(hikariDataSource.getHikariConfigMXBean());
    }

    @Command(description = "Show connection pool config",
            name = {"db", "pool", "config"},
            group = Constants.DB_COMMANDS)
    public void showPoolConfig() {
        logger.info("Connection pool config:\n%s"
                .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolConfig())));
    }

    @Command(description = "Show connection pool status",
            name = {"db", "pool", "status"},
            group = Constants.DB_COMMANDS)
    public void showPoolSize() {
        logger.info("Connection pool state:\n%s"
                .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolSize())));
    }

    @Command(description = "Set connection pool size",
            name = {"db", "pool", "size"},
            group = Constants.DB_COMMANDS)
    public void setPoolSize(@Option(description = "max pool size", required = true,
                                        longName = "maxSize") Integer maxSize,
                            @Option(description = "min idle size (same as max if omitted)",
                                    longName = "minIdle") Integer minIdle) {
        hikariDataSource.setMaximumPoolSize(maxSize);
        hikariDataSource.setMinimumIdle(minIdle != null ? minIdle : maxSize);

        logger.info("Set connection pool max size %d and min idle %d".formatted(maxSize, minIdle));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionPoolState {
        public int activeConnections;

        public int idleConnections;

        public int threadsAwaitingConnection;

        public int totalConnections;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionPoolConfig {
        public long connectionTimeout;

        public String poolName;

        public long idleTimeout;

        public long leakDetectionThreshold;

        public int maximumPoolSize;

        public long maxLifetime;

        public int minimumIdle;

        public long validationTimeout;
    }

    private static ConnectionPoolState from(HikariPoolMXBean bean) {
        ConnectionPoolState instance = new ConnectionPoolState();
        instance.activeConnections = bean.getActiveConnections();
        instance.idleConnections = bean.getIdleConnections();
        instance.threadsAwaitingConnection = bean.getThreadsAwaitingConnection();
        instance.totalConnections = bean.getTotalConnections();
        return instance;
    }

    private static ConnectionPoolConfig from(HikariConfigMXBean bean) {
        ConnectionPoolConfig instance = new ConnectionPoolConfig();
        instance.connectionTimeout = bean.getConnectionTimeout();
        instance.poolName = bean.getPoolName();
        instance.idleTimeout = bean.getIdleTimeout();
        instance.leakDetectionThreshold = bean.getLeakDetectionThreshold();
        instance.maximumPoolSize = bean.getMaximumPoolSize();
        instance.maxLifetime = bean.getMaxLifetime();
        instance.minimumIdle = bean.getMinimumIdle();
        instance.validationTimeout = bean.getValidationTimeout();
        return instance;
    }

}
