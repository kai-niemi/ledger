package io.cockroachdb.ledger.shell;

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

import io.cockroachdb.ledger.domain.ClusterInfo;
import io.cockroachdb.ledger.repository.RegionRepository;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.JsonHelper;

@Component
public class DatabasePoolCommands extends AbstractShellCommand {
    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegionRepository regionRepository;

    private ConnectionPoolState getConnectionPoolSize() {
        return from(hikariDataSource.getHikariPoolMXBean());
    }

    private ConnectionPoolConfig getConnectionPoolConfig() {
        return from(hikariDataSource.getHikariConfigMXBean());
    }

    @Command(description = "Show connection pool status",
            exitStatusExceptionMapper = "commandExceptionMapper",
            name = {"db", "show", "pool", "status"},
            group = Constants.DB_COMMANDS)
    public void showPoolSize(CommandContext commandContext) {
        commandContext.outputWriter().println(
                "Connection pool status: %s"
                        .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolSize())));
    }

    @Command(description = "Show connection pool size",
            exitStatusExceptionMapper = "commandExceptionMapper",
            name = {"db", "show", "pool", "size"},
            group = Constants.DB_COMMANDS)
    public void showPoolConfig(CommandContext commandContext) {
        commandContext.outputWriter().println(
                "Connection pool config: %s"
                        .formatted(JsonHelper.toFormattedJSON(objectMapper, getConnectionPoolConfig())));
    }

    @Command(description = "Set connection pool size",
            help = "Configure HikariCP pool size. Uses database metadata to infer the optimal pool size if default pool sizes are passed.",
            exitStatusExceptionMapper = "commandExceptionMapper",
            name = {"db", "set", "pool", "size"},
            alias = "sps",
            group = Constants.DB_COMMANDS)
    public void setPoolSize(@Option(description = "max pool size, default value means auto-configuration to sum(vCPUs)x4.", defaultValue = "-1",
                                    longName = "maxSize") Integer maxSize,
                            @Option(description = "min idle size (same as max size if unspecified)", defaultValue = "-1",
                                    longName = "minIdle") Integer minIdle,
                            CommandContext commandContext) {
        if (maxSize <= 0 || minIdle <= 0) {
            ClusterInfo clusterInfo = regionRepository.clusterInfo();
            maxSize = clusterInfo.getNumVCPUs() * 4;
            minIdle = maxSize;
        }

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
