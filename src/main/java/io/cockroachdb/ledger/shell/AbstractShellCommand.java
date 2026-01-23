package io.cockroachdb.ledger.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.shell.core.command.completion.CompositeCompletionProvider;
import org.springframework.shell.core.command.completion.EnumCompletionProvider;
import org.springframework.shell.jline.tui.component.SingleItemSelector;
import org.springframework.shell.jline.tui.component.support.SelectorItem;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.domain.SurvivalGoal;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.AccountServiceFacade;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.service.account.AccountPlanService;
import io.cockroachdb.ledger.shell.support.RegionProvider;
import static org.jline.jansi.AnsiConsole.getTerminal;

public abstract class AbstractShellCommand {
    protected static final String ACCOUNT_PLAN_EXIST = "accountPlanExist";

    protected static final String ACCOUNT_PLAN_NOT_EXIST = "accountPlanDoesNotExist";

    protected static final String ACCOUNT_TYPE_PROVIDER = "accountTypeProvider";

    protected static final String OPTION_EMPTY = "";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected AccountPlanService accountPlanService;

    @Autowired
    protected AccountServiceFacade accountServiceFacade;

    @Autowired
    protected RegionServiceFacade regionServiceFacade;

    @FunctionalInterface
    protected interface AccountQuery {
        List<AccountEntity> get(City city);
    }

    @Bean
    public CompletionProvider accountTypeProvider() {
        return new EnumCompletionProvider(AccountType.class, "--accountType");
    }

    @Bean
    public CompletionProvider survivalGoalProvider() {
        return new EnumCompletionProvider(SurvivalGoal.class, "--survivalGoal");
    }

    @Bean
    public CompletionProvider transferTypeProvider() {
        return new EnumCompletionProvider(TransferType.class, "--transferType");
    }

    @Bean
    public CompletionProvider regionProvider() {
        return new RegionProvider(regionServiceFacade, "--region");
    }

    @Bean
    public CompletionProvider accountTypeAndRegionCompletionProvider(
            @Autowired CompletionProvider accountTypeProvider,
            @Autowired CompletionProvider regionProvider) {
        return new CompositeCompletionProvider(accountTypeProvider, regionProvider);
    }

    public Availability accountPlanExist() {
        return accountPlanService.hasAccountPlan()
                ? Availability.available()
                : Availability.unavailable("there's no account plan!");
    }

    public Availability accountPlanDoesNotExist() {
        return !accountPlanService.hasAccountPlan()
                ? Availability.available()
                : Availability.unavailable("account plan already exist!");
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

    protected Optional<Pageable> askForPage(Page<?> page) {
        if (page.isEmpty()) {
            return Pageable.unpaged().toOptional();
        }

        List<SelectorItem<Pageable>> items = new ArrayList<>();

        items.add(SelectorItem.of("quit", Pageable.unpaged()));

        if (page.hasNext()) {
            items.add(SelectorItem.of("Next", page.nextOrLastPageable()));
        }
        if (page.hasPrevious()) {
            items.add(SelectorItem.of("Prev", page.previousOrFirstPageable()));
        }
        if (!page.isFirst()) {
            items.add(SelectorItem.of("First", PageRequest.of(0, page.getSize())));
        }
        if (!page.isLast()) {
            items.add(SelectorItem.of("Last", PageRequest.of(page.getTotalPages() - 1, page.getSize())));
        }

        SingleItemSelector<Pageable, SelectorItem<Pageable>> component
                = new SingleItemSelector<>(getTerminal(), items, "Select page", null);
//        component.setResourceLoader(getResourceLoader());
//        component.setTemplateExecutor(getTemplateExecutor());

        SingleItemSelector.SingleItemSelectorContext<Pageable, SelectorItem<Pageable>> context
                = component.run(SingleItemSelector.SingleItemSelectorContext.empty());

        return context.getResultItem()
                .flatMap(si -> Optional.of(si.getItem()));
    }
}
