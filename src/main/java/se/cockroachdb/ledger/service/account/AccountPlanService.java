package se.cockroachdb.ledger.service.account;

import java.util.Set;

import se.cockroachdb.ledger.model.AccountPlan;

public interface AccountPlanService {
    boolean hasAccountPlan();

    void buildAccountPlan(AccountPlan accountPlan, Set<String> visibleRegions);

    void dropAccountPlan(AccountPlan accountPlan);
}
