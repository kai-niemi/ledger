package io.cockroachdb.ledger.service.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import io.cockroachdb.ledger.annotations.ControlService;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.AccountPlan;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.service.AccountServiceFacade;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.shell.support.AnsiConsole;
import io.cockroachdb.ledger.util.AsciiArt;
import io.cockroachdb.ledger.util.Money;

@ControlService
public class DefaultAccountPlanService implements AccountPlanService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @Autowired
    private AccountServiceFacade accountServiceFacade;

    @Autowired
    private AnsiConsole ansiConsole;

    @Override
    public boolean hasAccountPlan() {
        return Boolean.TRUE.equals(this.jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM account_plan)", Boolean.class));
    }

    @Override
    public void buildAccountPlan(AccountPlan accountPlan) {
        Assert.isTrue(!hasAccountPlan(), "Account plan already exist!");

        logger.info("Building account plan '%s'".formatted(accountPlan.toString()));

        jdbcTemplate.update("insert into account_plan(name) values (?)", "default");

        final Set<City> cities = Region.joinCities(regionServiceFacade.listAllRegions());

        final AtomicInteger total = new AtomicInteger(cities.size() * accountPlan.getAccountsPerCity());
        final AtomicInteger current = new AtomicInteger();

        cities.parallelStream()
                .unordered()
                .forEach((city) -> {
                    Currency currency;
                    try {
                        currency = city.getCurrencyInstance();
                    } catch (IllegalArgumentException e) {
                        throw new ApplicationContextException("Bad currency code for city: " + city, e);
                    }

                    // Adjust proper fractions for currency
                    final Money initialBalance = Money.of(
                            BigDecimal.valueOf(accountPlan.getInitialBalance()), currency);
                    // Compute total balance for all accounts in this city
                    final Money totalBalance = initialBalance.multiply(accountPlan.getAccountsPerCity()).negate();

                    accountServiceFacade.createAccount(
                            AccountEntity.builder()
                                    .withGeneratedId()
                                    .withCity(city.getName())
                                    .withName("system-account-" + city.getName().toLowerCase())
                                    .withAllowNegative(true)
                                    .withBalance(totalBalance)
                                    .withAccountType(AccountType.LIABILITY)
                                    .withUpdated(LocalDateTime.now()).build());

                    // Use set returning (virtual table) function for speed

                    int rows = jdbcTemplate.update(
                            "insert into account (city, balance, currency, name, type, closed, allow_negative, updated_at) "
                            + "select "
                            + " ?,"
                            + " ?,"
                            + " ?,"
                            + " (concat('user:', no::text)),"
                            + " ?::account_type,"
                            + " false,"
                            + " 0,"
                            + " ? "
                            + "from generate_series(1, ?) no",
                            city.getName(),
                            initialBalance.getAmount(),
                            initialBalance.getCurrency().getCurrencyCode(),
                            AccountType.ASSET.getCode(),
                            LocalDateTime.now(),
                            accountPlan.getAccountsPerCity());

                    // Tick

                    ansiConsole.progressBar(current.addAndGet(rows), total.get(),
                            city.getCountry() + "/" + city.getName() + "/" + initialBalance + " ("
                            + currency.getSymbol(Locale.US) + ")");
                });

        logger.info("Ledger is open for business %s".formatted(AsciiArt.happy()));
    }

    @Override
    public void dropAccountPlan(AccountPlan accountPlan) {
        logger.info("Dropping existing account plan");

        List<String> tables = List.of(
                "transfer_item",
                "transfer",
                "account",
                "account_plan"
        );

        AtomicInteger current = new AtomicInteger();

        tables.forEach(table -> {
            ansiConsole.progressBar(current.incrementAndGet(), tables.size(), table);
            jdbcTemplate.execute("truncate table %s CASCADE".formatted(table));
        });

        logger.info("Finished dropping account plan");
    }
}
