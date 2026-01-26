package io.cockroachdb.ledger.shell;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.workload.Worker;
import io.cockroachdb.ledger.service.workload.WorkloadDescription;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.util.DurationUtils;
import io.cockroachdb.ledger.util.Money;
import io.cockroachdb.ledger.util.RandomData;

@Component
public class WorkloadBalanceCommands extends AbstractShellCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @Command(exitStatusExceptionMapper = "commandExceptionMapper", description = "Account balance reads",
            help = "Start the account balance read workload.",
            name = {"workload", "start", "read-balance"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            completionProvider = "regionProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void readBalance(
            @Option(description = Constants.ACCOUNT_LIMIT_HELP,
                    defaultValue = Constants.DEFAULT_ACCOUNT_LIMIT,
                    longName = "limit") int limit,
            @Option(description = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    longName = "region") String region,
            @Option(description = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION,
                    longName = "duration") String duration,
            @Option(description = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1",
                    longName = "concurrency") int concurrency,
            @Option(description = "enable stale, historical follower reads",
                    defaultValue = "false",
                    longName = "stale") boolean stale,
            CommandContext commandContext
    ) {
        final Map<City, List<UUID>> accountIdsPerCity = findCityAccountIDs(region, city -> {
            return accountServiceFacade.findAccounts(city,
                    AccountType.ASSET,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            commandContext.outputWriter()
                    .println("No cities found matching region '%s'".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, uuids) -> {
            workloadManager.submitWorkers(
                    new Worker<Money>() {
                        @Override
                        public Money call() {
                            if (stale) {
                                return accountServiceFacade.getAccountBalanceSnapshot(RandomData.selectRandom(uuids));
                            }
                            return accountServiceFacade.getAccountBalance(RandomData.selectRandom(uuids));
                        }

                        @Override
                        public boolean test(Integer x) {
                            return Instant.now().isBefore(stopTime);
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Read Balance" + (stale ? " (stale)" : "");
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    }, concurrency);
        });
    }
}
