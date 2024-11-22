package se.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import se.cockroachdb.ledger.model.AccountBatch;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.workload.WorkloadDescription;
import se.cockroachdb.ledger.workload.Worker;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.util.DurationUtils;

import java.time.Instant;
import java.util.List;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_COMMANDS)
public class AccountCommands extends AbstractServiceCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Create new zero-balance asset accounts in batches",
            key = {"create-accounts", "ca"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXISTS)
    public void createAccounts(
            @ShellOption(help = "batch size",
                    defaultValue = "128") int batchSize,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration,
            @ShellOption(help = Constants.EXECUTION_LIMIT_HELP,
                    defaultValue = "-1") int limit
    ) {
        // List of cities in potentially different countries and corresponding currencies
        final List<City> allCities = Region.joinCities(regionServiceFacade.listRegions(region));

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        allCities.forEach(city -> {
            workloadManager.submitWorker(
                    new Worker<>() {
                        @Override
                        public Void call() {
                            AccountBatch batch = new AccountBatch();
                            batch.setCity(city);
                            batch.setBatchSize(batchSize);

                            accountServiceFacade.createAccountBatch(batch);

                            return null;
                        }

                        @Override
                        public boolean test(Integer x) {
                            return limit > 0 ? (x > limit || Instant.now().isBefore(stopTime))
                                    : Instant.now().isBefore(stopTime);
                        }
                    },
                    new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Create Accounts";
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    });
        });
    }
}
