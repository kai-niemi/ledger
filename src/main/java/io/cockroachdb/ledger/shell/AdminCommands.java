package io.cockroachdb.ledger.shell;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

import io.cockroachdb.ledger.config.DataSourceConfig;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.util.DurationUtils;
import io.cockroachdb.ledger.util.NetworkAddress;

@Component
public class AdminCommands extends AbstractShellCommand {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Toggle SQL trace logging",
            name = {"admin", "sql", "trace"},
            group = Constants.ADMIN_COMMANDS)
    public void toggleSqlTraceLogging(CommandContext commandContext) {
        boolean enabled = toggleLogLevel(DataSourceConfig.SQL_TRACE_LOGGER);
        commandContext.outputWriter()
                .println("SQL Trace Logging %s".formatted(enabled ? "ENABLED" : "DISABLED"));
    }

    private boolean toggleLogLevel(String name) {
        ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) LoggerFactory
                .getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(name);
        if (logger.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.DEBUG)) {
            logger.setLevel(Level.TRACE);
            return true;
        } else {
            logger.setLevel(Level.DEBUG);
            return false;
        }
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Exit the shell",
            name = {"admin", "quit"},
            alias = "q",
            group = Constants.ADMIN_COMMANDS)
    public void quit() {
        SpringApplication.exit(applicationContext, () -> 0);
        System.exit(0);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Print application uptime",
            name = {"admin", "uptime"},
            group = Constants.ADMIN_COMMANDS)
    public void uptime(CommandContext commandContext) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        commandContext.outputWriter()
                .println(DurationUtils.millisecondsToDisplayString(uptime));
    }

    @Command(description = "Print local IP addresses",
            name = {"admin", "ip"},
            group = Constants.ADMIN_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void printIP(CommandContext commandContext) {
        commandContext.outputWriter().println(
                """
                        
                                    Local IP: %s
                                 External IP: %s
                                    Hostname: %s
                        Hostname (canonical): %s
                              Local API root: %s
                           External API root: %s""".formatted(
                        NetworkAddress.getLocalIP(),
                        NetworkAddress.getExternalIP(),
                        NetworkAddress.getHostname(),
                        NetworkAddress.getCanonicalHostName(),
                        "http://%s:%d".formatted(NetworkAddress.getLocalIP(), serverPort),
                        "http://%s:%d".formatted(NetworkAddress.getExternalIP(), serverPort)
                ));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Print system information",
            name = {"admin", "info"},
            group = Constants.ADMIN_COMMANDS)
    public void systemInfo(CommandContext commandContext) {
        PrintWriter pw = commandContext.outputWriter();

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        pw.println(">> OS");
        pw.println(" Arch: %s | OS: %s | Version: %s".formatted(os.getArch(), os.getName(), os.getVersion()));
        pw.println(" Available processors: %d".formatted(os.getAvailableProcessors()));
        pw.println(" Load avg: %f".formatted(os.getSystemLoadAverage()));

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        pw.println(">> Runtime");
        pw.println(" Pid: %s".formatted(r.getPid()));
        pw.println(" Uptime: %s".formatted(r.getUptime()));
        pw.println(" VM name: %s | Vendor: %s | Version: %s"
                .formatted(r.getVmName(), r.getVmVendor(), r.getVmVersion()));

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        pw.println(">> Runtime");
        pw.println(" Peak threads: %d".formatted(t.getPeakThreadCount()));
        pw.println(" Thread #: %d".formatted(t.getThreadCount()));
        pw.println(" Total started threads: %d".formatted(t.getTotalStartedThreadCount()));

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            pw.println(" Thread (%d): %s %s".formatted(value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            ));
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        pw.println(">> Memory");
        pw.println(" Heap: %s".formatted(m.getHeapMemoryUsage().toString()));
        pw.println(" Non-heap: %s".formatted(m.getNonHeapMemoryUsage().toString()));
        pw.println(" Pending GC: %s".formatted(m.getObjectPendingFinalizationCount()));
    }
}
