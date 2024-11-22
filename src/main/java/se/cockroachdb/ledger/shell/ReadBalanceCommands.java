package se.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.util.Money;
import se.cockroachdb.ledger.workload.WorkloadDescription;
import se.cockroachdb.ledger.workload.Worker;
import se.cockroachdb.ledger.workload.WorkloadManager;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.util.DurationUtils;
import se.cockroachdb.ledger.util.RandomData;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_COMMANDS)
public class ReadBalanceCommands extends AbstractServiceCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Read account balances", key = {"read-balance", "rb"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXISTS)
    public void readBalance(
            @ShellOption(help = Constants.ACCOUNT_LIMIT_HELP,
                    defaultValue = Constants.DEFAULT_ACCOUNT_LIMIT) int limit,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.CITY_NAME_HELP,
                    defaultValue = ShellOption.NULL) String cityName,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration
    ) {
        final Map<City, List<UUID>> accountIdsPerCity = findAccounts(region, cityName, cities -> {
            return accountServiceFacade.findAccounts(cities, AccountType.ASSET,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found matching region '%s'".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, uuids) -> {
            workloadManager.submitWorker(
                    new Worker<Money>() {
                        @Override
                        public Money call() {
                            return accountServiceFacade.getAccountBalance(RandomData.selectRandom(uuids));
                        }

                        @Override
                        public boolean test(Integer x) {
                            return Instant.now().isBefore(stopTime);
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Read Balance";
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    });
        });
    }

    @ShellMethod(value = "Read historical account balances", key = {"read-balance-historical", "rbh"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXISTS)
    public void readHistoricalBalance(
            @ShellOption(help = Constants.ACCOUNT_LIMIT_HELP,
                    defaultValue = Constants.DEFAULT_ACCOUNT_LIMIT) int limit,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.CITY_NAME_HELP,
                    defaultValue = ShellOption.NULL) String cityName,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration
    ) {
        final Map<City, List<UUID>> accountIdsPerCity = findAccounts(region, cityName, cities -> {
            return accountServiceFacade.findAccounts(cities, AccountType.ASSET,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found matching region '%s'".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, uuids) -> {
            workloadManager.submitWorker(
                    new Worker<Money>() {
                        @Override
                        public Money call() {
                            return accountServiceFacade.getAccountBalanceSnapshot(RandomData.selectRandom(uuids));
                        }

                        @Override
                        public boolean test(Integer x) {
                            return Instant.now().isBefore(stopTime);
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Read Balance Snapshot";
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    });
        });
    }
}
