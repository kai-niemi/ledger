package io.cockroachdb.ledger.shell;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.RegionProvider;
import io.cockroachdb.ledger.util.DurationUtils;
import io.cockroachdb.ledger.util.Money;
import io.cockroachdb.ledger.util.RandomData;
import io.cockroachdb.ledger.service.workload.Worker;
import io.cockroachdb.ledger.service.workload.WorkloadDescription;
import io.cockroachdb.ledger.service.workload.WorkloadManager;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_START_COMMANDS)
public class ReadBalanceCommands extends AbstractServiceCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Read account balances", key = {"read-balance", "rb"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXIST)
    public void readBalance(
            @ShellOption(help = Constants.ACCOUNT_LIMIT_HELP,
                    defaultValue = Constants.DEFAULT_ACCOUNT_LIMIT) int limit,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration,
            @ShellOption(help = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1") int concurrency
    ) {
        final Map<City, List<UUID>> accountIdsPerCity = findCityAccountIDs(region, city -> {
            return accountServiceFacade.findAccounts(city,
                    AccountType.ASSET,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found matching region '%s'".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, uuids) -> {
            workloadManager.submitWorkers(
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
                    }, concurrency);
        });
    }

    @ShellMethod(value = "Read historical account balances", key = {"read-balance-historical", "rbh"})
    @ShellMethodAvailability(AbstractServiceCommand.ACCOUNT_PLAN_EXIST)
    public void readHistoricalBalance(
            @ShellOption(help = Constants.ACCOUNT_LIMIT_HELP,
                    defaultValue = Constants.DEFAULT_ACCOUNT_LIMIT) int limit,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION) String duration,
            @ShellOption(help = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1") int concurrency
    ) {
        final Map<City, List<UUID>> accountIdsPerCity = findCityAccountIDs(region, city -> {
            return accountServiceFacade.findAccounts(city,
                    AccountType.ASSET,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found matching region '%s'".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, uuids) -> {
            workloadManager.submitWorkers(
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
                    }, concurrency);
        });
    }
}
