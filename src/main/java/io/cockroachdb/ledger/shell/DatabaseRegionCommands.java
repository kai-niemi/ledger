package io.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.BeanListTableModel;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.domain.SurvivalGoal;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.ListTableModel;
import io.cockroachdb.ledger.shell.support.TableUtils;

@Component
public class DatabaseRegionCommands extends AbstractShellCommand {
    @Command(description = "Set primary database region",
            name = {"db", "region", "primary", "set"},
            completionProvider = "regionProvider",
            group = Constants.DB_COMMANDS)
    public void setPrimaryRegion(@Option(description = "region name", required = true,
            longName = "region") String region) {
        regionServiceFacade.setPrimaryRegion(region);
    }

    @Command(description = "Show primary region",
            name = {"db", "region", "primary", "show"},
            group = Constants.DB_COMMANDS)
    public void showPrimaryRegion(CommandContext commandContext) {
        regionServiceFacade.getPrimaryRegion().ifPresentOrElse(region -> {
            commandContext.outputWriter().println(printRegionTable(List.of(region)));
        }, () -> {
            commandContext.outputWriter().println("No primary region found");
        });
    }

    @Command(description = "Set secondary database region",
            name = {"db", "region", "secondary", "set"},
            completionProvider = "regionProvider",
            group = Constants.DB_COMMANDS)
    public void secondaryRegion(@Option(description = "region name", required = true,
            longName = "region") String region) {
        regionServiceFacade.setSecondaryRegion(region);
    }

    @Command(description = "Show secondary region",
            name = {"db", "region", "primary", "show"},
            group = Constants.DB_COMMANDS)
    public void showSecondaryRegion(CommandContext commandContext) {
        regionServiceFacade.getSecondaryRegion().ifPresentOrElse(region -> {
            commandContext.outputWriter().println(printRegionTable(List.of(region)));
        }, () -> {
            commandContext.outputWriter().println("No secondary region found");
        });
    }

    @Command(description = "Drop secondary region, if any",
            name = {"db", "region", "secondary", "drop"},
            group = Constants.DB_COMMANDS)
    public void dropSecondaryRegion() {
        regionServiceFacade.dropSecondaryRegion();
    }

    @Command(description = "Add configured database regions",
            name = {"db", "region", "add"},
            group = Constants.DB_COMMANDS)
    public void addRegions() {
        regionServiceFacade.addDatabaseRegions();
    }

    @Command(description = "Drop existing database regions",
            name = {"db", "region", "drop"},
            group = Constants.DB_COMMANDS)
    public void dropRegions() {
        regionServiceFacade.dropDatabaseRegions();
    }

    @Command(description = "Set database survival goal",
            name = {"db", "survival", "set"},
            completionProvider = "survivalGoalProvider",
            group = Constants.DB_COMMANDS)
    public void setSurvivalGoal(@Option(description = "survival goal",
            longName = "goal") SurvivalGoal goal) {
        regionServiceFacade.setSurvivalGaol(goal);
    }

    @Command(description = "Show survival goal",
            name = {"db", "region", "survival", "show"},
            group = Constants.DB_COMMANDS)
    public void showSurvivalGoal(CommandContext commandContext) {
        commandContext.outputWriter().println(regionServiceFacade.getSurvivalGoal());
    }

    @Command(description = "Apply multi-region configurations (regions, localities and survival goals)",
            name = {"db", "region", "multi", "apply"},
            completionProvider = "survivalGoalProvider",
            group = Constants.DB_COMMANDS)
    public void applyMultiRegion(@Option(description = "survival goal", defaultValue = "ZONE",
            longName = "goal") SurvivalGoal goal) {
        regionServiceFacade.applyMultiRegion(goal);
    }

    @Command(description = "Revert multi-region configurations (regions, localities and survival goals)",
            name = {"db", "region", "multi", "revert"},
            group = Constants.DB_COMMANDS)
    public void revertMultiRegion() {
        regionServiceFacade.revertMultiRegion();
    }


    @Command(description = "List all regions",
            name = {"db", "region", "list"},
            group = Constants.DB_COMMANDS)
    public void listRegions(CommandContext commandContext) {
        commandContext.outputWriter()
                .println(printRegionTable(regionServiceFacade.listAllRegions()));
    }

    @Command(description = "List region cities",
            name = {"db", "region", "city", "list"},
            completionProvider = "regionCompletionProvider",
            group = Constants.DB_COMMANDS)
    public void listRegionCities(
            @Option(description = "region name (gateway region if empty)",
                    defaultValue = "gateway", required = true,
                    longName = "region") String region,
            CommandContext commandContext) {
        Set<City> cities = regionServiceFacade.listCities(region);

        commandContext.outputWriter()
                .println(TableUtils.prettyPrint(new ListTableModel<>(
                        cities, List.of("Name"), (object, column) -> column == 0 ? object : "??")));
    }

    @Command(description = "Show gateway region",
            name = {"db", "region", "gateway"},
            group = Constants.DB_COMMANDS)
    public void showGatewayRegion(CommandContext commandContext) {
        regionServiceFacade.getGatewayRegion()
                .ifPresentOrElse(region -> commandContext.outputWriter()
                                .println(printRegionTable(List.of(region))),
                        () -> commandContext.outputWriter().println("No gateway region found"));
    }

    @Command(description = "Show CREATE TABLE statement",
            name = {"db", "region", "table", "show"},
            completionProvider = "tableNameProvider",
            group = Constants.DB_COMMANDS)
    public void showCreateTable(@Option(description = "table name",
                                            longName = "table") TableName table,
                                CommandContext commandContext) {
        commandContext.outputWriter().println(regionServiceFacade.showCreateTable(table.name()));
    }

    public static String printRegionTable(List<Region> regions) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("name", "Name");
        header.put("cityNames", "Cities");
        header.put("databaseRegions", "Database Regions");
        header.put("primary", "Primary");
        header.put("secondary", "Secondary");

        return TableUtils.prettyPrint(new BeanListTableModel<>(regions, header));
    }
}
