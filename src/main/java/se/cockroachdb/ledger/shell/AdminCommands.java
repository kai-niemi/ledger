package se.cockroachdb.ledger.shell;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import ch.qos.logback.classic.Level;

import se.cockroachdb.ledger.config.DataSourceConfig;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.util.DurationUtils;

@ShellComponent
@ShellCommandGroup(Constants.ADMIN_COMMANDS)
public class AdminCommands implements Quit.Command {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @ShellMethod(value = "Toggle SQL trace logging (server side)", key = {"sql-trace", "r"})
    public void toggleSqlTraceLogging() {
        boolean enabled = toggleLogLevel(DataSourceConfig.SQL_TRACE_LOGGER);
        logger.info("SQL Trace Logging {}", enabled ? "ENABLED" : "DISABLED");
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

    @ShellMethod(value = "Exit the shell", key = {"quit", "exit", "q"})
    public void quit() {
        logger.info("Quitting");
        SpringApplication.exit(applicationContext, () -> 0);
        System.exit(0);
    }

    @ShellMethod(value = "Print application uptime")
    public void uptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        logger.info("%s".formatted(DurationUtils.millisecondsToDisplayString(uptime)));
    }

    @ShellMethod(value = "Print system information", key = {"system-info", "si"})
    public void systemInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        logger.info(">> OS");
        logger.info(" Arch: %s | OS: %s | Version: %s".formatted(os.getArch(), os.getName(), os.getVersion()));
        logger.info(" Available processors: %d".formatted(os.getAvailableProcessors()));
        logger.info(" Load avg: %f".formatted(os.getSystemLoadAverage()));

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        logger.info(">> Runtime");
        logger.info(" Uptime: %s".formatted(r.getUptime()));
        logger.info(
                " VM name: %s | Vendor: %s | Version: %s".formatted(r.getVmName(), r.getVmVendor(), r.getVmVersion()));

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        logger.info(">> Runtime");
        logger.info(" Peak threads: %d".formatted(t.getPeakThreadCount()));
        logger.info(" Thread #: %d".formatted(t.getThreadCount()));
        logger.info(" Total started threads: %d".formatted(t.getTotalStartedThreadCount()));

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            logger.info(" Thread (%d): %s %s".formatted(value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            ));
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        logger.info(">> Memory");
        logger.info(" Heap: %s".formatted(m.getHeapMemoryUsage().toString()));
        logger.info(" Non-heap: %s".formatted(m.getNonHeapMemoryUsage().toString()));
        logger.info(" Pending GC: %s".formatted(m.getObjectPendingFinalizationCount()));
    }
}
