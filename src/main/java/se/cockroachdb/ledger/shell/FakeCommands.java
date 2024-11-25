package se.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import se.cockroachdb.ledger.workload.WorkloadDescription;
import se.cockroachdb.ledger.workload.Worker;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.util.DurationUtils;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_START_COMMANDS)
public class FakeCommands {
    private final AtomicInteger monotonicCounter = new AtomicInteger();

    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Create fake workload with random sleep durations", key = {"fake", "f"})
    public void createFakeWorkloads(
            @ShellOption(help = "number of workloads",
                    defaultValue = "5") int count,
            @ShellOption(help = "min sleep time in ms",
                    defaultValue = "10") int min,
            @ShellOption(help = "max sleep time in ms",
                    defaultValue = "150") int max,
            @ShellOption(help = "error probability (0-1)",
                    defaultValue = "0.0") double probability,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration
    ) {
        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        IntStream.rangeClosed(1, count).forEach(value -> {
            final int n = monotonicCounter.incrementAndGet();

            workloadManager.submitWorker(
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
                            return "Sleeper #" + n;
                        }

                        @Override
                        public String categoryValue() {
                            return "Fakes";
                        }
                    });
        });
    }

}
