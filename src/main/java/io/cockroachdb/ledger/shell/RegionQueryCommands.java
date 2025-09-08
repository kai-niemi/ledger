package io.cockroachdb.ledger.shell;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;

import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.shell.support.Constants;
import io.cockroachdb.ledger.shell.support.ListTableModel;
import io.cockroachdb.ledger.shell.support.RegionProvider;
import io.cockroachdb.ledger.shell.support.TableUtils;

@ShellComponent
@ShellCommandGroup(Constants.REGION_QUERY_COMMANDS)
public class RegionQueryCommands extends AbstractShellComponent {
    public static String printRegionTable(List<Region> regions) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("name", "Name");
        header.put("cityNames", "Cities");
        header.put("databaseRegions", "Database Regions");
        header.put("primary", "Primary");
        header.put("secondary", "Secondary");

        return TableUtils.prettyPrint(new BeanListTableModel<>(regions, header));
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @ShellMethod(value = "List regions", key = {"list-regions", "lr"})
    public void listRegions() {
        logger.info("\n{}", printRegionTable(regionServiceFacade.listAllRegions()));
    }

    @ShellMethod(value = "List region cities", key = {"list-cities", "lc"})
    public void listRegionCities(
            @ShellOption(help = "region name (gateway region if empty)",
                    defaultValue = "gateway",
                    valueProvider = RegionProvider.class) String region) {
        Set<City> cities = regionServiceFacade.listCities(region);

        logger.info("\n" + TableUtils.prettyPrint(
                new ListTableModel<>(cities, List.of("Name"), (object, column) -> column == 0 ? object : "??")));
    }

    @ShellMethod(value = "Show gateway region", key = {"show-gateway-region", "sgr"})
    public void showGatewayRegion() {
        regionServiceFacade.getGatewayRegion()
                .ifPresentOrElse(region -> logger.info("\n{}", printRegionTable(List.of(region))),
                        () -> logger.warn("No gateway region found"));
    }

    @ShellMethod(value = "Show primary region", key = {"show-primary-region", "spr"})
    public void showPrimaryRegion() {
        regionServiceFacade.getPrimaryRegion().ifPresentOrElse(region -> {
            logger.info("\n" + printRegionTable(List.of(region)));
        }, () -> {
            logger.warn("No primary region found");
        });
    }

    @ShellMethod(value = "Show secondary region", key = {"show-secondary-region", "ssr"})
    public void showSecondaryRegion() {
        regionServiceFacade.getSecondaryRegion().ifPresentOrElse(region -> {
            logger.info("\n" + printRegionTable(List.of(region)));
        }, () -> {
            logger.warn("No secondary region found");
        });
    }


    @ShellMethod(value = "Show survival goal", key = {"show-survival-goal", "ssg"})
    public void showSurvivalGoal() {
        logger.info("" + regionServiceFacade.getSurvivalGoal());
    }
    @ShellMethod(value = "Show CREATE TABLE statement", key = {"show-create-table", "sc"})
    public void showCreateTable(@ShellOption(help = "table name",
            valueProvider = EnumValueProvider.class) TableName table) {
        logger.info("\n" + regionServiceFacade.showCreateTable(table.name()));
    }

}
