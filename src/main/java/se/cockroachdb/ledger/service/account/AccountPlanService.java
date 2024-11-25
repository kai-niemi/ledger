package se.cockroachdb.ledger.service.account;

import se.cockroachdb.ledger.model.AccountPlan;

public interface AccountPlanService {
    boolean hasAccountPlan();

    void buildAccountPlan(AccountPlan accountPlan);

    void dropAccountPlan(AccountPlan accountPlan);
}
