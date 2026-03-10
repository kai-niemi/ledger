package io.cockroachdb.ledger.service.account;

import io.cockroachdb.ledger.domain.AccountPlan;

public interface AccountPlanService {
    boolean hasAccountPlan();

    void buildAccountPlan(AccountPlan accountPlan);

    void dropAccountPlan(AccountPlan accountPlan);
}
