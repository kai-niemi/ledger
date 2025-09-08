package io.cockroachdb.ledger.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import io.cockroachdb.ledger.annotations.ServiceFacade;
import io.cockroachdb.ledger.annotations.TransactionImplicit;
import io.cockroachdb.ledger.domain.AccountBatchRequest;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.account.AccountService;
import io.cockroachdb.ledger.util.CockroachFacts;
import io.cockroachdb.ledger.util.Money;

@ServiceFacade
public class AccountServiceFacade {
    private static final AtomicInteger monotonicBatchSequence = new AtomicInteger(1);

    @Autowired
    private AccountService accountService;

    @TransactionImplicit
    public AccountEntity createAccount(AccountEntity accountEntity) {
        return accountService.createAccount(accountEntity);
    }

    @TransactionImplicit
    public List<UUID> createAccountBatch(AccountBatchRequest form) {
        return accountService.createAccountBatch(() -> AccountEntity.builder()
                        .withId(UUID.randomUUID())
                        .withCity(form.getCity())
                        .withAccountType(form.getAccountType())
                        .withBalance(Money.zero(form.getCurrency()))
                        .withAllowNegative(false)
                        .withName(String.format("user:%05d", monotonicBatchSequence.incrementAndGet()))
                        .withDescription(CockroachFacts.nextFact(256))
                        .build(),
                form.getBatchSize());
    }

    @TransactionImplicit(readOnly = true)
    public Page<AccountEntity> findAccounts(AccountType accountType, Pageable pageable) {
        return accountService.findAll(accountType, pageable);
    }

//    @TransactionImplicit(readOnly = true)
//    public List<AccountEntity> findAccounts(Set<String> cities,
//                                            AccountType accountType,
//                                            Pair<BigDecimal, BigDecimal> range,
//                                            int limit) {
//        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
//                "Expecting no active transaction");
//
//        List<Callable<List<AccountEntity>>> tasks = new ArrayList<>();
//        tasks.add(() -> accountService.findByCriteria(cities, accountType, range, limit));
//
//        List<AccountEntity> accountEntities = new ArrayList<>();
//
//        ConcurrencyUtils.runConcurrentlyAndWait(tasks,
//                ConcurrencyUtils.UNBOUNDED_CONCURRENCY, accountEntities::addAll);
//
//        return accountEntities;
//    }

    @TransactionImplicit(readOnly = true)
    public List<AccountEntity> findAccounts(City city, AccountType accountType,
                                      Pair<BigDecimal, BigDecimal> range,
                                      int limit) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting no active transaction");

        return accountService.findByCriteria(Set.of(city.getName()), accountType, range, limit);
    }

        @TransactionImplicit(readOnly = true)
    public Money getAccountBalance(UUID id) {
        return accountService.getBalance(id);
    }

    @TransactionImplicit(readOnly = true)
    public Money getAccountBalanceSnapshot(UUID id) {
        return accountService.getBalanceSnapshot(id);
    }

}
