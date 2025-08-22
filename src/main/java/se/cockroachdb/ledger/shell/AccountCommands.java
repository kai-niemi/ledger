package se.cockroachdb.ledger.shell;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.model.AccountBatch;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.util.DurationUtils;
import se.cockroachdb.ledger.workload.Worker;
import se.cockroachdb.ledger.workload.WorkloadDescription;
import se.cockroachdb.ledger.workload.WorkloadManager;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_START_COMMANDS)
public class AccountCommands extends AbstractServiceCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Create new zero-balance asset accounts in batches",
            key = {"create-accounts", "ca"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXIST)
    public void createAccounts(
            @ShellOption(help = "batch size",
                    defaultValue = "128") int batchSize,
            @ShellOption(help = "account type (any but LIABILITY)",
                    defaultValue = "ASSET",
                    valueProvider = EnumValueProvider.class) AccountType accountType,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration,
            @ShellOption(help = Constants.ITERATIONS_HELP,
                    defaultValue = "-1") int iterations
    ) {
        if (accountType.equals(AccountType.LIABILITY)) {
            throw new IllegalArgumentException("You are not allowed to create accounts of this type!");
        }

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
                            batch.setAccountType(accountType);

                            accountServiceFacade.createAccountBatch(batch);

                            return null;
                        }

                        @Override
                        public boolean test(Integer x) {
                            return iterations > 0 ? x < iterations
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
