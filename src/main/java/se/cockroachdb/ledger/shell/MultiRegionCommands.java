package se.cockroachdb.ledger.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import se.cockroachdb.ledger.model.SurvivalGoal;
import se.cockroachdb.ledger.model.TableName;
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;

import java.util.List;

import static se.cockroachdb.ledger.shell.RegionCommands.printRegionTable;

@ShellComponent
@ShellCommandGroup(Constants.MULTI_REGION_COMMANDS)
public class MultiRegionCommands {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @ShellMethod(value = "Show primary region", key = {"show-primary-region", "spr"})
    public void showPrimaryRegion() {
        regionServiceFacade.getPrimaryRegion().ifPresentOrElse(region -> {
            logger.info("\n" + printRegionTable(List.of(region)));
        }, () -> {
            logger.warn("No primary region found");
        });
    }

    @ShellMethod(value = "Set primary database region", key = {"primary-region", "pr"})
    public void primaryRegion(@ShellOption(help = "region name",
            valueProvider = RegionProvider.class) String region) {
        regionServiceFacade.setPrimaryRegion(region);
    }

    @ShellMethod(value = "Show secondary region", key = {"show-secondary-region", "ssr"})
    public void showSecondaryRegion() {
        regionServiceFacade.getSecondaryRegion().ifPresentOrElse(region -> {
            logger.info("\n" + printRegionTable(List.of(region)));
        }, () -> {
            logger.warn("No secondary region found");
        });
    }

    @ShellMethod(value = "Set secondary database region", key = {"secondary-region", "sr"})
    public void secondaryRegion(@ShellOption(help = "region name",
            valueProvider = RegionProvider.class) String region) {
        regionServiceFacade.setSecondaryRegion(region);
    }

    @ShellMethod(value = "Drop secondary region", key = {"drop-secondary", "ds"})
    public void dropSecondaryRegion() {
        regionServiceFacade.dropSecondaryRegion();
    }

    @ShellMethod(value = "Add database regions", key = {"add-regions", "ar"})
    public void addRegions() {
        regionServiceFacade.addDatabaseRegions();
    }

    @ShellMethod(value = "Drop database regions", key = {"drop-regions", "dr"})
    public void dropRegions() {
        regionServiceFacade.dropDatabaseRegions();
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

    @ShellMethod(value = "Set survival goal", key = {"survival-goal", "sg"})
    public void survivalGoal(@ShellOption(help = "survival goal",
            valueProvider = EnumValueProvider.class) SurvivalGoal goal) {
        regionServiceFacade.setSurvivalGaol(goal);
    }

    @ShellMethod(value = "Apply multi-region configurations (regions, localities and survival goals)",
            key = {"apply-multi-region", "amr"})
    public void applyMultiRegion() {
        regionServiceFacade.applyMultiRegion();
    }

    @ShellMethod(value = "Revert multi-region configurations (regions, localities and survival goals)",
            key = {"revert-multi-region", "rmr"})
    public void revertMultiRegion() {
        regionServiceFacade.revertMultiRegion();
    }
}
