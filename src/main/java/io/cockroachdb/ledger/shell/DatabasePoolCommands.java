package io.cockroachdb.ledger.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.CommandContext;
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

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Show connection pool status",
            name = {"db", "show", "pool", "status"},
            group = Constants.DB_COMMANDS)
    public void showPoolSize(CommandContext commandContext) {
        commandContext.outputWriter().println(
                "Connection pool state: %s"
                        .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolSize())));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Show connection pool size",
            name = {"db", "show", "pool", "size"},
            group = Constants.DB_COMMANDS)
    public void showPoolConfig(CommandContext commandContext) {
        commandContext.outputWriter().println(
                "Connection pool config: %s"
                        .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolConfig())));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Set connection pool size",
            name = {"db", "set", "pool", "size"},
            group = Constants.DB_COMMANDS)
    public void setPoolSize(@Option(description = "max pool size", required = true,
                                    longName = "maxSize") Integer maxSize,
                            @Option(description = "min idle size (same as max if omitted)",
                                    longName = "minIdle", required = true) Integer minIdle,
                            CommandContext commandContext) {
        hikariDataSource.setMaximumPoolSize(maxSize);
        hikariDataSource.setMinimumIdle(minIdle);

        commandContext.outputWriter().println(
                "Set connection pool max size: %d min idle: %d".formatted(maxSize, minIdle));
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
