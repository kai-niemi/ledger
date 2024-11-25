package se.cockroachdb.ledger.shell;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.Assert;

import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.TransferRequest;
import se.cockroachdb.ledger.service.TransferServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.util.CockroachFacts;
import se.cockroachdb.ledger.util.DurationUtils;
import se.cockroachdb.ledger.util.Money;
import se.cockroachdb.ledger.util.RandomData;
import se.cockroachdb.ledger.workload.Worker;
import se.cockroachdb.ledger.workload.WorkloadDescription;
import se.cockroachdb.ledger.workload.WorkloadManager;

@ShellComponent
@ShellCommandGroup(Constants.WORKLOAD_START_COMMANDS)
public class TransferCommands extends AbstractServiceCommand {
    @Autowired
    private TransferServiceFacade transferServiceFacade;

    @Autowired
    private WorkloadManager workloadManager;

    @ShellMethod(value = "Transfer funds between non-negative balance asset accounts", key = {"transfer-funds", "tf"})
    @ShellMethodAvailability(ACCOUNT_PLAN_EXISTS)
    public void transferFunds(
            @ShellOption(help = "minimum transfer amount in account currency",
                    defaultValue = "0.05") final double min,
            @ShellOption(help = "maximum transfer amount in account currency",
                    defaultValue = "10.00") final double max,
            @ShellOption(help = "number of legs per transfer (must be at least 2)",
                    defaultValue = "2") final int legs,
            @ShellOption(help = "additional number of legs per transfer",
                    defaultValue = "0") final int variance,
            @ShellOption(help = "minimum account balance for inclusion",
                    defaultValue = "100.00") final double minBalance,
            @ShellOption(help = "maximum account balance for inclusion",
                    defaultValue = "999999999.00") final double maxBalance,
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
        if (legs < 2) {
            logger.info("Number of legs must be >= 2");
            return;
        }

        final Map<City, List<UUID>> accountIdsPerCity = findAccounts(region, cityName, cities -> {
            return accountServiceFacade.findAccounts(cities, AccountType.ASSET,
                    Pair.of(BigDecimal.valueOf(minBalance), BigDecimal.valueOf(maxBalance)),
                    limit);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No cities found matching criteria");
            return;
        }

        final Instant stopTime = Instant.now().plus(DurationUtils.parseDuration(duration));

        accountIdsPerCity.forEach((city, accounts) -> {
            workloadManager.submitWorker(
                    new Worker<Transfer>() {
                        @Override
                        public Transfer call() {
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
                    });
        });
    }

    private Transfer transferFunds(City city,
                                   List<UUID> accountIds,
                                   double min,
                                   double max,
                                   int legs,
                                   int variance) {
        Currency currency = Currency.getInstance(city.getCurrency());

        Money transferAmount = RandomData.randomMoneyBetween(min, max, currency);

        TransferRequest.Builder builder = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity(city.getName())
                .withTransferType(TransferType.BANK)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now().plusDays(1));

        int nLegs = variance > 0 ? ThreadLocalRandom.current()
                .nextInt(legs, legs + variance) : legs;

        // Ensure an even number
        if (nLegs % 2 != 0) {
            nLegs++;
        }

        IntStream.rangeClosed(1, nLegs).forEach(value -> {
            // Possible non-unique id:s but the legs are coalesced
            UUID accountId = RandomData.selectRandom(accountIds);
            builder.addItem()
                    .withId(accountId)
                    .withCity(city.getName())
                    .withAmount(value % 2 == 0 ? transferAmount.negate() : transferAmount)
                    .withNote(CockroachFacts.nextFact())
                    .then();
        });

        return transferServiceFacade.createTransfer(builder.build());
    }

    @ShellMethod(value = "Transfer grants from liability accounts to user accounts", key = {
            "transfer-grants", "tg"})
    @ShellMethodAvailability(ACCOUNT_PLAN_EXISTS)
    public void transferGrants(
            @ShellOption(help = "transfer amount debited liability accounts and credited asset accounts",
                    defaultValue = "500.00") final double amount,
            @ShellOption(help = "minimum asset account balance for inclusion",
                    defaultValue = "0.00") final double minBalance,
            @ShellOption(help = "maximum asset account balance for inclusion",
                    defaultValue = "50.00") final double maxBalance,
            @ShellOption(help = "max number of transfer legs per batch",
                    defaultValue = "128") final int legs,
            @ShellOption(help = "target account type (any but LIABILITY)",
                    defaultValue = "ASSET",
                    valueProvider = EnumValueProvider.class) AccountType accountType,
            @ShellOption(help = Constants.REGIONS_HELP,
                    defaultValue = Constants.DEFAULT_REGION,
                    valueProvider = RegionProvider.class) String region,
            @ShellOption(help = Constants.CITY_NAME_HELP,
                    defaultValue = ShellOption.NULL) String cityName
    ) {
        if (accountType.equals(AccountType.LIABILITY)) {
            throw new IllegalArgumentException("You are not allowed to target accounts of this type!");
        }

        final Map<City, List<UUID>> accountIdsPerCity = findAccounts(region, cityName, cities -> {
            return accountServiceFacade.findAccounts(cities, AccountType.LIABILITY,
                    Pair.of(BigDecimal.ZERO, BigDecimal.ZERO),
                    8192);
        });

        if (accountIdsPerCity.isEmpty()) {
            logger.warn("No liability accounts found matching criteria");
            return;
        }

        accountIdsPerCity.forEach((city, liabilityAccounts) -> {
            workloadManager.submitWorker(
                    new Worker<Transfer>() {
                        List<Account> accountPage;

                        @Override
                        public Transfer call() {
                            Assert.notNull(accountPage, "accountPage is null");
                            if (logger.isTraceEnabled()) {
                                logger.trace("Processing %,d asset accounts for city '%s'"
                                        .formatted(accountPage.size(), city.getName()));
                            }
                            List<UUID> assetAccounts = accountPage.stream().map(Account::getId).toList();
                            return grantFunds(city, liabilityAccounts, assetAccounts, BigDecimal.valueOf(amount));
                        }

                        @Override
                        public boolean test(Integer x) {
                            accountPage = accountServiceFacade.findAccounts(city, accountType,
                                    Pair.of(BigDecimal.valueOf(minBalance), BigDecimal.valueOf(maxBalance)), legs);
                            return !accountPage.isEmpty();
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
                    });
        });
    }

    private Transfer grantFunds(City city,
                                List<UUID> liabilityAccounts,
                                List<UUID> assetAccounts,
                                BigDecimal amount) {
        Currency currency = Currency.getInstance(city.getCurrency());

        Money transferAmount = Money.of(amount, currency);

        TransferRequest.Builder builder = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity(city.getName())
                .withTransferType(TransferType.GRANT)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now().plusDays(3));

        final UUID id = RandomData.selectRandom(liabilityAccounts);

        builder.addItem()
                .withId(id)
                .withCity(city.getName())
                .withAmount(transferAmount.multiply(assetAccounts.size()).negate())
                .withNote("Grant from " + id)
                .then();

        assetAccounts.forEach(uuid -> {
            builder.addItem()
                    .withId(uuid)
                    .withCity(city.getName())
                    .withAmount(transferAmount)
                    .withNote(CockroachFacts.nextFact())
                    .then();
        });

        return transferServiceFacade.createTransfer(builder.build());
    }
}
