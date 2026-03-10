package io.cockroachdb.ledger.shell;

import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cockroachdb.ledger.domain.AccountPlan;
import io.cockroachdb.ledger.domain.ApplicationProperties;
import io.cockroachdb.ledger.repository.RegionRepository;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.JsonHelper;

@Component
public class DatabaseCommands extends AbstractShellCommand {
    @Autowired
    private ApplicationProperties applicationModel;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Command(
            description = "Print database information",
            exitStatusExceptionMapper = "commandExceptionMapper",
            name = {"db", "info"},
            group = Constants.DB_COMMANDS)
    public void databaseInfo(CommandContext commandContext) {
        PrintWriter pw = commandContext.outputWriter();
        pw.println("Database version: " + regionRepository.databaseVersion());
        pw.println("Transaction isolation: " + regionRepository.databaseIsolation());
        pw.println("Gateway region: " + regionRepository.getGatewayRegion().orElse("n/a"));
        pw.println("Primary region: " + regionRepository.getPrimaryRegion().orElse("n/a"));
        pw.println("Secondary region: " + regionRepository.getSecondaryRegion().orElse("n/a"));
        pw.println("Cluster regions: " + regionRepository.listClusterRegions());
        pw.println("Database regions: " + regionRepository.listDatabaseRegions());
        pw.println("Cluster info: " + JsonHelper.toFormattedJSON(objectMapper, regionRepository.clusterInfo()));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Show CREATE TABLE statement",
            name = {"db", "show", "table"},
            completionProvider = "tableNameProvider",
            group = Constants.DB_COMMANDS)
    public void showCreateTable(@Option(description = "table name", longName = "tableName", required = true) TableName table,
                                CommandContext commandContext) {
        commandContext.outputWriter()
                .println(regionServiceFacade.showCreateTable(table.name()));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Build account plan",
            name = {"db", "build", "accountplan"},
            availabilityProvider = ACCOUNT_PLAN_NOT_EXIST,
            group = Constants.DB_COMMANDS)
    public void buildAccountPlan(
            @Option(description = "number of accounts per city",
                    defaultValue = "5000",
                    longName = "accounts") String accounts,
            @Option(description = "initial balance per account in city currency",
                    longName = "initialBalance",
                    defaultValue = "5000.00") Double initialBalance
    ) {
        AccountPlan accountPlan = applicationModel.getAccountPlan();
        accountPlan.setAccountsPerCity(accounts);
        accountPlan.setInitialBalance(initialBalance);
        accountPlanService.buildAccountPlan(accountPlan);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Drop account plan",
            help = "Drop plan including all created accounts and transfers (destructive)",
            name = {"db", "drop", "accountplan"},
            availabilityProvider = ACCOUNT_PLAN_EXIST,
            group = Constants.DB_COMMANDS)
    public void dropAccountPlan(@Option(description = "confirm dropping accounts and transfers",
            required = true,
            longName = "confirm") boolean confirm,
                                CommandContext commandContext) {
        if (confirm) {
            AccountPlan accountPlan = applicationModel.getAccountPlan();
            accountPlanService.dropAccountPlan(accountPlan);
        } else {
            commandContext.outputWriter().println(
                    "You need to confirm this operation!");
        }
    }
}
