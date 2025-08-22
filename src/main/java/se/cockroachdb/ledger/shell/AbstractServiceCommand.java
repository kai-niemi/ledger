package se.cockroachdb.ledger.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.service.AccountServiceFacade;
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.service.account.AccountPlanService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<Account> findAccounts(List<City> cities);
    }

    protected Map<City, List<UUID>> findAccounts(String region, String cityName, AccountQuery accountQuery) {
        // List of cities in potentially different countries and corresponding currencies
        final List<City> allCities = Region.joinCities(regionServiceFacade.listRegions(region));

        // Map of city names to list of account IDs
        final Map<City, List<UUID>> accountIdsPerCity = new HashMap<>();
        {
            Map<String, List<Account>> accountsPerCity = accountQuery
                    .findAccounts(allCities).stream().collect(Collectors.groupingBy(Account::getCity));

            accountsPerCity.forEach((city, accountEntities) -> {
                City.findByName(allCities, city).ifPresent(c -> {
                    accountIdsPerCity.put(c, accountEntities.stream().map(Account::getId).toList());
                });
            });
        }

        // Narrow to one city
        if (cityName != null) {
            City.findByName(allCities, cityName).ifPresent(city -> {
                accountIdsPerCity.clear();

                List<UUID> accounts = accountIdsPerCity.getOrDefault(city, List.of());

                if (!accounts.isEmpty()) {
                    accountIdsPerCity.put(city, accounts);
                }
            });
        }

        return accountIdsPerCity;
    }

}
