package io.cockroachdb.ledger.shell;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferRequest;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.TransferServiceFacade;
import io.cockroachdb.ledger.service.workload.Worker;
import io.cockroachdb.ledger.service.workload.WorkloadDescription;
import io.cockroachdb.ledger.service.workload.WorkloadManager;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.util.CockroachFacts;
import io.cockroachdb.ledger.util.DurationUtils;
import io.cockroachdb.ledger.util.Money;
import io.cockroachdb.ledger.util.RandomData;

@Component
public class WorkloadTransferCommands extends AbstractShellCommand {
    @Autowired
    private TransferServiceFacade transferServiceFacade;

    @Autowired
    private WorkloadManager workloadManager;

    @Command(description = "Transfer funds between non-negative balance asset accounts",
            name = {"workload", "start", "transfer", "funds"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            completionProvider = "regionProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void transferFunds(
            @Option(description = "minimum transfer amount in account currency",
                    defaultValue = "0.05",
                    longName = "min") final double min,
            @Option(description = "maximum transfer amount in account currency",
                    defaultValue = "10.00",
                    longName = "max") final double max,
            @Option(description = "number of legs per transfer (must be at least 2)",
                    defaultValue = "2",
                    longName = "legs") final int legs,
            @Option(description = "additional number of legs per transfer",
                    defaultValue = "0",
                    longName = "variance") final int variance,
            @Option(description = "minimum account balance for inclusion",
                    defaultValue = "100.00",
                    longName = "minBalance") final double minBalance,
            @Option(description = "maximum account balance for inclusion",
                    defaultValue = "999999999.00",
                    longName = "maxBalance") final double maxBalance,
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
                    longName = "concurrency") int concurrency
    ) {
        if (legs < 2) {
            logger.info("Number of legs must be >= 2");
            return;
        }

        final Map<City, List<UUID>> accountIdsPerCity = findCityAccountIDs(region, city -> {
            return accountServiceFacade.findAccounts(city,
                    AccountType.ASSET,
                    Pair.of(BigDecimal.valueOf(minBalance), BigDecimal.valueOf(maxBalance)),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found in region '%s' (region mapping may be needed)".formatted(region));
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, accounts) -> {
            workloadManager.submitWorkers(
                    new Worker<TransferEntity>() {
                        @Override
                        public TransferEntity call() {
                            return transferFunds(city, accounts, min, max, legs, variance);
                        }

                        @Override
                        public boolean test(Integer x) {
                            return Instant.now().isBefore(stopTime);
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Transfer Funds";
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    }, concurrency);
        });
    }

    private TransferEntity transferFunds(City city,
                                         List<UUID> accountIds,
                                         double min,
                                         double max,
                                         int legs,
                                         int variance) {
        Currency currency = Currency.getInstance(city.getCurrency());

        Money transferAmount = RandomData.randomMoneyBetween(min, max, currency);

        TransferRequest.Builder builder = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity(city)
                .withTransferType(TransferType.BANK)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now().plusDays(1));

        int nLegs = variance > 0 ? ThreadLocalRandom.current()
                .nextInt(legs, legs + variance) : legs;

        // Ensure an even number
        if (nLegs % 2 != 0) {
            nLegs++;
        }

        AtomicInteger c = new AtomicInteger();

        RandomData.selectRandomUnique(accountIds, nLegs)
                .forEach(uuid -> builder.addItem()
                        .withId(uuid)
                        .withAmount(c.incrementAndGet() % 2 == 0 ? transferAmount.negate() : transferAmount)
                        .withNote(CockroachFacts.nextFact())
                        .then());

        return transferServiceFacade.createTransfer(builder.build());
    }

    @Command(description = "Transfer grants from liability accounts to user accounts",
            name = {"workload", "start", "transfer", "grants"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            completionProvider = "accountTypeAndRegionCompletionProvider",
            group = Constants.WORKLOAD_COMMANDS)
    public void transferGrants(
            @Option(description = "transfer amount debited liability accounts and credited asset accounts",
                    defaultValue = "500.00",
                    longName = "amount") final double amount,
            @Option(description = "minimum asset account balance for inclusion",
                    defaultValue = "0.00",
                    longName = "minBalance") final double minBalance,
            @Option(description = "maximum asset account balance for inclusion",
                    defaultValue = "50.00",
                    longName = "maxBalance") final double maxBalance,
            @Option(description = "max number of transfer legs per batch",
                    defaultValue = "128",
                    longName = "legs") final int legs,
            @Option(description = "target account type (any but LIABILITY)",
                    defaultValue = "ASSET",
                    longName = "accountType") AccountType accountType,
            @Option(description = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    longName = "region") String region,
            @Option(description = "concurrency level, i.e. number of threads to start per city",
                    defaultValue = "1",
                    longName = "concurrency") int concurrency
    ) {
        if (accountType.equals(AccountType.LIABILITY)) {
            throw new IllegalArgumentException("You are not allowed to target accounts of this type!");
        }

        final Map<City, List<UUID>> accountIdsPerCity = findCityAccountIDs(region, city -> {
            return accountServiceFacade.findAccounts(city,
                    AccountType.LIABILITY,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    8192);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No liability accounts found matching criteria");
            return;
        }

        accountIdsPerCity.forEach((city, liabilityAccounts) -> {
            workloadManager.submitWorkers(
                    new Worker<TransferEntity>() {
                        List<AccountEntity> accountEntityPage;

                        @Override
                        public TransferEntity call() {
                            Assert.notNull(accountEntityPage, "accountPage is null");
                            if (logger.isTraceEnabled()) {
                                logger.trace("Processing %,d asset accounts for city '%s'"
                                        .formatted(accountEntityPage.size(), city));
                            }
                            List<UUID> assetAccounts = accountEntityPage.stream().map(AccountEntity::getId).toList();
                            return grantFunds(city, liabilityAccounts, assetAccounts, BigDecimal.valueOf(amount));
                        }

                        @Override
                        public boolean test(Integer x) {
                            accountEntityPage = accountServiceFacade.findAccounts(city, accountType,
                                    Pair.of(BigDecimal.valueOf(minBalance), BigDecimal.valueOf(maxBalance)), legs);
                            return !accountEntityPage.isEmpty();
                        }
                    }, new WorkloadDescription() {
                        @Override
                        public String displayValue() {
                            return "Transfer Grants";
                        }

                        @Override
                        public String categoryValue() {
                            return city.getName();
                        }
                    }, concurrency);
        });
    }

    private TransferEntity grantFunds(City city,
                                      List<UUID> liabilityAccounts,
                                      List<UUID> assetAccounts,
                                      BigDecimal amount) {
        Currency currency = Currency.getInstance(city.getCurrency());

        Money transferAmount = Money.of(amount, currency);

        TransferRequest.Builder builder = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity(city)
                .withTransferType(TransferType.GRANT)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now().plusDays(3));

        final UUID id = RandomData.selectRandom(liabilityAccounts);

        builder.addItem()
                .withId(id)
                .withAmount(transferAmount.multiply(assetAccounts.size()).negate())
                .withNote("Grant from " + id)
                .then();

        assetAccounts.forEach(uuid -> {
            builder.addItem()
                    .withId(uuid)
                    .withAmount(transferAmount)
                    .withNote(CockroachFacts.nextFact())
                    .then();
        });

        return transferServiceFacade.createTransfer(builder.build());
    }
}
