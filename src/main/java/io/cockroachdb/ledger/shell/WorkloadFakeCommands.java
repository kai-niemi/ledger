package io.cockroachdb.ledger.shell;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.service.workload.Worker;
import io.cockroachdb.ledger.service.workload.WorkloadDescription;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.util.DurationUtils;

@Component
public class WorkloadFakeCommands extends AbstractShellCommand {
    private final AtomicInteger monotonicCounter = new AtomicInteger();

    @Autowired
    private WorkloadManager workloadManager;

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Start fake workload with random sleep durations",
            name = {"workload", "start", "fake"},
            group = Constants.WORKLOAD_COMMANDS)
    public void createFakeWorkloads(
            @Option(description = "number of workloads",
                    defaultValue = "5",
                    longName = "count") Integer count,
            @Option(description = "min sleep time in ms",
                    defaultValue = "10",
                    longName = "min") Integer min,
            @Option(description = "max sleep time in ms",
                    defaultValue = "150",
                    longName = "max") Integer max,
            @Option(description = "error probability (0-1)",
                    defaultValue = "0.0",
                    longName = "probability") Double probability,
            @Option(description = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION,
                    longName = "duration") String duration,
            @Option(description = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1",
                    longName = "concurrency") Integer concurrency
    ) {
        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        IntStream.rangeClosed(1, count).forEach(value -> {
            final int n = monotonicCounter.incrementAndGet();

            workloadManager.submitWorkers(
                    new Worker<Void>() {
                        @Override
                        public Void call() throws Exception {
                            ThreadLocalRandom r = ThreadLocalRandom.current();
                            if (r.nextDouble(0, 1.0) < probability) {
                                throw new IllegalStateException("Fake exception from #" + n);
                            } else {
                                long t = r.nextLong(min, max);
                                TimeUnit.MILLISECONDS.sleep(t);
                            }
                            return null;
                        }

                        @Override
                        public boolean test(Integer x) {
                            return Instant.now().isBefore(stopTime);
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Fake #" + n;
                        }

                        @Override
                        public String categoryValue() {
                            return "Fakes";
                        }
                    }, concurrency);
        });
    }

}
