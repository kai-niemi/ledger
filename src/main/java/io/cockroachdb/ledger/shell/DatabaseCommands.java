package io.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.model.AccountPlan;
import io.cockroachdb.ledger.model.ApplicationProperties;
import io.cockroachdb.ledger.repository.RegionRepository;
import io.cockroachdb.ledger.shell.support.Constants;

@Component
public class DatabaseCommands extends AbstractShellCommand {
    @Autowired
    private ApplicationProperties applicationModel;

    @Autowired
    private RegionRepository regionRepository;

    @Command(description = "Print database information",
            name = {"db", "info"},
            group = Constants.DB_COMMANDS)
    public void databaseInfo() {
        logger.info("Database version: " + regionRepository.databaseVersion());
        logger.info("Transaction isolation: " + regionRepository.databaseIsolation());
        logger.info("Gateway region: " + regionRepository.getGatewayRegion().orElse("n/a"));
        logger.info("Primary region: " + regionRepository.getPrimaryRegion().orElse("n/a"));
        logger.info("Secondary region: " + regionRepository.getSecondaryRegion().orElse("n/a"));
        logger.info("Cluster regions: " + regionRepository.listClusterRegions());
        logger.info("Database regions: " + regionRepository.listDatabaseRegions());
    }

    @Command(description = "Build account plan",
            name = {"db", "accountplan", "build"},
            availabilityProvider = ACCOUNT_PLAN_NOT_EXIST,
            group = Constants.DB_COMMANDS)
    public void buildAccountPlan(
            @Option(description = "override number of accounts per city", defaultValue = OPTION_EMPTY,
                    longName = "accounts") String accounts,
            @Option(description = "override initial balance per account (amount)",
                    longName = "initialBalance") Double initialBalance
    ) {
        AccountPlan accountPlan = applicationModel.getAccountPlan();
        if (OPTION_EMPTY.equals(accounts)) {
            accountPlan.setAccountsPerCity(accounts);
        }
        if (initialBalance != null) {
            accountPlan.setInitialBalance(initialBalance);
        }
        accountPlanService.buildAccountPlan(accountPlan);
    }

    @Command(description = "Drop account plan",
            help = "Drop plan including all created accounts and transfers (destructive)",
            name = {"db", "accountplan", "drop"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            group = Constants.DB_COMMANDS)
    public void dropAccountPlan(@Option(description = "confirm dropping accounts and transfers", required = true,
            longName = "confirm") boolean confirm) {
        if (confirm) {
            AccountPlan accountPlan = applicationModel.getAccountPlan();
            accountPlanService.dropAccountPlan(accountPlan);
        } else {
            logger.warn("You need to confirm this operation!");
        }
    }
}
