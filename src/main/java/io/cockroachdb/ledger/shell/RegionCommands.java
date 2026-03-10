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
import io.cockroachdb.ledger.domain.City;
import io.cockroachdb.ledger.domain.Region;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.ListTableModel;
import io.cockroachdb.ledger.shell.support.TableUtils;

@Component
public class RegionCommands extends AbstractShellCommand {
    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Set primary database region",
            name = {"region", "set", "primary"},
            completionProvider = "regionProvider",
            group = Constants.REGION_COMMANDS)
    public void setPrimaryRegion(@Option(description = "region name", required = true,
            longName = "region") String region) {
        regionServiceFacade.setPrimaryRegion(region);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Show primary region",
            name = {"region", "show", "primary"},
            group = Constants.REGION_COMMANDS)
    public void showPrimaryRegion(CommandContext commandContext) {
        regionServiceFacade.getPrimaryRegion().ifPresentOrElse(region -> {
            commandContext.outputWriter().println(printRegionTable(List.of(region)));
        }, () -> {
            commandContext.outputWriter().println("No primary region found");
        });
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Set secondary database region",
            name = {"region", "set", "secondary"},
            completionProvider = "regionProvider",
            group = Constants.REGION_COMMANDS)
    public void secondaryRegion(@Option(description = "region name", required = true,
            longName = "region") String region) {
        regionServiceFacade.setSecondaryRegion(region);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Show secondary region",
            name = {"region", "show", "secondary"},
            group = Constants.REGION_COMMANDS)
    public void showSecondaryRegion(CommandContext commandContext) {
        regionServiceFacade.getSecondaryRegion().ifPresentOrElse(region -> {
            commandContext.outputWriter().println(printRegionTable(List.of(region)));
        }, () -> {
            commandContext.outputWriter().println("No secondary region found");
        });
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Drop secondary region, if any",
            name = {"region", "drop", "secondary"},
            group = Constants.REGION_COMMANDS)
    public void dropSecondaryRegion() {
        regionServiceFacade.dropSecondaryRegion();
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Add configured database regions",
            name = {"region", "add"},
            group = Constants.REGION_COMMANDS)
    public void addRegions() {
        regionServiceFacade.addDatabaseRegions();
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Drop existing database regions",
            name = {"region", "drop", "all"},
            group = Constants.REGION_COMMANDS)
    public void dropRegions() {
        regionServiceFacade.dropDatabaseRegions();
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Set database survival goal",
            name = {"region", "set", "survival"},
            completionProvider = "survivalGoalProvider",
            group = Constants.REGION_COMMANDS)
    public void setSurvivalGoal(@Option(description = "survival goal",
            longName = "goal") SurvivalGoal goal) {
        regionServiceFacade.setSurvivalGaol(goal);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Show survival goal",
            name = {"region", "show", "survival"},
            group = Constants.REGION_COMMANDS)
    public void showSurvivalGoal(CommandContext commandContext) {
        commandContext.outputWriter().println(regionServiceFacade.getSurvivalGoal());
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Apply multi-region configurations (regions, localities and survival goals)",
            name = {"region", "apply", "multi-region"},
            completionProvider = "survivalGoalProvider",
            group = Constants.REGION_COMMANDS)
    public void applyMultiRegion(@Option(description = "survival goal", defaultValue = "ZONE",
            longName = "goal") SurvivalGoal goal) {
        regionServiceFacade.applyMultiRegion(goal);
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Revert multi-region configurations (regions, localities and survival goals)",
            name = {"region", "revert", "multi-region"},
            group = Constants.REGION_COMMANDS)
    public void revertMultiRegion() {
        regionServiceFacade.revertMultiRegion();
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "List all regions",
            name = {"region", "list"},
            group = Constants.REGION_COMMANDS)
    public void listRegions(CommandContext commandContext) {
        commandContext.outputWriter()
                .println(printRegionTable(regionServiceFacade.listAllRegions()));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "List region cities",
            name = {"region", "list", "city"},
            completionProvider = "regionProvider",
            group = Constants.REGION_COMMANDS)
    public void listRegionCities(
            @Option(description = "region name (gateway region if empty)",
                    defaultValue = "gateway", longName = "region") String region,
            CommandContext commandContext) {
        Set<City> cities = regionServiceFacade.listCities(region);
        commandContext.outputWriter()
                .println(TableUtils.prettyPrint(new ListTableModel<>(
                        cities, List.of("Name"), (object, column) -> column == 0 ? object : "??")));
    }

    @Command(exitStatusExceptionMapper = "commandExceptionMapper",
            description = "Show gateway region",
            name = {"region", "show", "gateway"},
            group = Constants.REGION_COMMANDS)
    public void showGatewayRegion(CommandContext commandContext) {
        regionServiceFacade.getGatewayRegion()
                .ifPresentOrElse(region -> commandContext.outputWriter()
                                .println(printRegionTable(List.of(region))),
                        () -> commandContext.outputWriter().println("No gateway region found"));
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
