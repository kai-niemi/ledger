package io.cockroachdb.ledger.shell;

import java.time.Instant;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.domain.AccountBatchRequest;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.domain.City;
import io.cockroachdb.ledger.service.workload.Worker;
import io.cockroachdb.ledger.service.workload.WorkloadDescription;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.util.DurationUtils;

@Component
public class WorkloadAccountCommands extends AbstractShellCommand {
    @Autowired
    private WorkloadManager workloadManager;

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Create new zero-balance asset accounts in batches",
            name = {"workload", "start", "create-accounts"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            group = Constants.WORKLOAD_COMMANDS,
            completionProvider = "accountTypeAndRegionCompletionProvider")
    public void createAccounts(
            @Option(description = "batch size",
                    defaultValue = "128",
                    longName = "batchSize") Integer batchSize,
            @Option(description = "account type (any but LIABILITY)",
                    defaultValue = "ASSET",
                    longName = "accountType") AccountType accountType,
            @Option(description = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    longName = "region") String region,
            @Option(description = Constants.DURATION_HELP,
                    defaultValue = Constants.DEFAULT_DURATION,
                    longName = "duration") String duration,
            @Option(description = Constants.ITERATIONS_HELP,
                    defaultValue = "-1",
                    longName = "iterations") Integer iterations,
            @Option(description = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1",
                    longName = "concurrency") Integer concurrency
    ) {
        if (accountType.equals(AccountType.LIABILITY)) {
            throw new IllegalArgumentException("You are not allowed to create accounts of this type!");
        }

        // List of cities in potentially different countries and corresponding currencies
        Set<City> cities = regionServiceFacade.listCities(region);

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        cities.forEach(city -> {
            workloadManager.submitWorkers(
                    new Worker<>() {
                        @Override
                        public Void call() {
                            AccountBatchRequest batch = new AccountBatchRequest();
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
                    }, concurrency);
        });
    }

}
