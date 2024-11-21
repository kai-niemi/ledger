package se.cockroachdb.ledger.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.JsonHelper;

@ShellComponent
@ShellCommandGroup(Constants.CONNECTION_POOL_COMMANDS)
public class PoolCommands extends AbstractShellComponent {
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HikariDataSource hikariDataSource;

    private ConnectionPoolState getConnectionPoolSize() {
        return from(hikariDataSource.getHikariPoolMXBean());
    }

    private ConnectionPoolConfig getConnectionPoolConfig() {
        return from(hikariDataSource.getHikariConfigMXBean());
    }

    @ShellMethod(value = "Show connection pool config", key = {"show-pool-config", "spc"})
    public void showPoolConfig() {
        logger.info("Connection pool config:\n%s"
                .formatted(JsonHelper.toFormattedJSON(getConnectionPoolConfig())));
    }

    @ShellMethod(value = "Show connection pool status", key = {"show-pool-status", "sps"})
    public void showPoolSize() {
        logger.info("Connection pool state:\n%s"
                .formatted(JsonHelper.toFormattedJSON(getConnectionPoolSize())));
    }

    @ShellMethod(value = "Set connection pool sizes", key = {"pool-size", "ps"})
    public void setPoolSize(@ShellOption(help = "min idle") int minIdle,
                            @ShellOption(help = "max size") int maxSize) {
        hikariDataSource.setMinimumIdle(minIdle);
        hikariDataSource.setMaximumPoolSize(maxSize);

        logger.info("Set connection pool max size %d and min idle to %d".formatted(maxSize, minIdle));
    }
}
