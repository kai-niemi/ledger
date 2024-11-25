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
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.shell.support.RegionProvider;

@ShellComponent
@ShellCommandGroup(Constants.REGION_MODIFICATION_COMMANDS)
public class RegionModifyCommands {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @ShellMethod(value = "Set primary database region", key = {"primary-region", "pr"})
    public void primaryRegion(@ShellOption(help = "region name",
            valueProvider = RegionProvider.class) String region) {
        regionServiceFacade.setPrimaryRegion(region);
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

    @ShellMethod(value = "Set survival goal", key = {"survival-goal", "sg"})
    public void survivalGoal(@ShellOption(help = "survival goal",
            valueProvider = EnumValueProvider.class) SurvivalGoal goal) {
        regionServiceFacade.setSurvivalGaol(goal);
    }

    @ShellMethod(value = "Apply multi-region configurations (regions, localities and survival goals)",
            key = {"apply-multi-region", "amr"})
    public void applyMultiRegion(@ShellOption(help = "survival goal",
            valueProvider = EnumValueProvider.class) SurvivalGoal goal) {
        regionServiceFacade.applyMultiRegion(goal);
    }

    @ShellMethod(value = "Revert multi-region configurations (regions, localities and survival goals)",
            key = {"revert-multi-region", "rmr"})
    public void revertMultiRegion() {
        regionServiceFacade.revertMultiRegion();
    }
}
