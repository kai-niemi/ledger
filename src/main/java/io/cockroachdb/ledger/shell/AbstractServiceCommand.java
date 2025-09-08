package io.cockroachdb.ledger.shell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.AccountServiceFacade;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.service.account.AccountPlanService;

@ShellComponent
public abstract class AbstractServiceCommand extends AbstractInteractiveCommand {
    public static final String ACCOUNT_PLAN_EXIST = "accountPlanExist";

    public static final String ACCOUNT_PLAN_NOT_EXIST = "accountPlanDoesNotExist";

    @Autowired
    protected AccountPlanService accountPlanService;

    @Autowired
    protected AccountServiceFacade accountServiceFacade;

    @Autowired
    protected RegionServiceFacade regionServiceFacade;

    public Availability accountPlanExist() {
        return accountPlanService.hasAccountPlan()
                ? Availability.available()
                : Availability.unavailable("there's no account plan (use 'help build-account-plan')!");
    }

    public Availability accountPlanDoesNotExist() {
        return !accountPlanService.hasAccountPlan()
                ? Availability.available()
                : Availability.unavailable("account plan already exist!");
    }

    @FunctionalInterface
    protected interface AccountQuery {
        List<AccountEntity> get(City city);
    }

    protected Map<City, List<UUID>> findCityAccountIDs(String region, AccountQuery accountQuery) {
        Map<City, List<UUID>> regionAccountIDs = new HashMap<>();

        regionServiceFacade.listCities(region).forEach(city -> {
            regionAccountIDs.put(city,
                    accountQuery.get(city).stream()
                            .map(AccountEntity::getId)
                            .toList());
        });

        return regionAccountIDs;
    }
}
