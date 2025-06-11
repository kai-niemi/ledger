package se.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import se.cockroachdb.ledger.model.ApplicationProperties;
import se.cockroachdb.ledger.model.AccountPlan;
import se.cockroachdb.ledger.shell.support.Constants;
import se.cockroachdb.ledger.util.Multiplier;

@ShellComponent
@ShellCommandGroup(Constants.PLAN_COMMANDS)
public class PlanBuilderCommands extends AbstractServiceCommand {
    @Autowired
    private ApplicationProperties applicationModel;

    @ShellMethod(value = "Build account plan", key = {"build-account-plan", "bap"})
    public void buildAccountPlan(
            @ShellOption(help = "override number of accounts per city", defaultValue = ShellOption.NULL)
            String accounts,
            @ShellOption(help = "override initial balance per account (amount)", defaultValue = ShellOption.NULL)
            Double initialBalance
    ) {
        AccountPlan accountPlan = applicationModel.getAccountPlan();
        if (ShellOption.NULL.equals(accounts)) {
            accountPlan.setAccountsPerCity(Multiplier.parseInt(accounts));
        }
        if (initialBalance != null) {
            accountPlan.setInitialBalance(initialBalance);
        }
        accountPlanService.buildAccountPlan(accountPlan);
    }

    @ShellMethod(value = "Drop account plan including all accounts and transfers",
            key = {"drop-account-plan", "dap"})
    public void dropAccountPlan(@ShellOption(help = "confirm dropping accounts and transfers")
                                boolean confirm) {
        if (confirm) {
            AccountPlan accountPlan = applicationModel.getAccountPlan();
            accountPlanService.dropAccountPlan(accountPlan);
        } else {
            logger.warn("You need to confirm this operation!");
        }
    }
}
