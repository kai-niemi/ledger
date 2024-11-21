package se.cockroachdb.ledger.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.ListTableModel;
import se.cockroachdb.ledger.shell.support.RegionProvider;
import se.cockroachdb.ledger.shell.support.TableUtils;

import java.util.LinkedHashMap;
import java.util.List;

@ShellComponent
@ShellCommandGroup(Constants.REGION_COMMANDS)
public class RegionCommands extends AbstractShellComponent {
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

    @ShellMethod(value = "Show gateway region", key = {"show-gateway-region", "sgr"})
    public void showGatewayRegion() {
        regionServiceFacade.getGatewayRegion()
                .ifPresentOrElse(region -> logger.info("\n{}", printRegionTable(List.of(region))),
                        () -> logger.warn("No gateway region found"));
    }

    @ShellMethod(value = "List regions", key = {"list-regions", "lr"})
    public void listRegions() {
        logger.info("\n{}", printRegionTable(regionServiceFacade.listAllRegions()));
    }

    @ShellMethod(value = "List region cities", key = {"list-cities", "lc"})
    public void listRegionCities(
            @ShellOption(help = "region name (gateway region if empty)",
                    defaultValue = "gateway",
                    valueProvider = RegionProvider.class) String region) {
        final List<City> cities = Region.joinCities(regionServiceFacade.listRegions(region));

        logger.info("\n" + TableUtils.prettyPrint(
                new ListTableModel<>(cities, List.of("Name"), (object, column) -> column == 0 ? object : "??")));
    }
}