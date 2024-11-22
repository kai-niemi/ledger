package se.cockroachdb.ledger.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import se.cockroachdb.ledger.annotations.ServiceFacade;
import se.cockroachdb.ledger.annotations.TransactionExplicit;
import se.cockroachdb.ledger.annotations.TransactionImplicit;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.model.AccountBatch;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.service.account.AccountService;
import se.cockroachdb.ledger.util.CockroachFacts;
import se.cockroachdb.ledger.util.ConcurrencyUtils;
import se.cockroachdb.ledger.util.Money;

@ServiceFacade
public class AccountServiceFacade {
    private static final AtomicInteger monotonicBatchSequence = new AtomicInteger(1);

    @Autowired
    private AccountService accountService;

    @TransactionImplicit
    public Account createAccount(Account account) {
        return accountService.createAccount(account);
    }

    @TransactionImplicit
    public List<UUID> createAccountBatch(AccountBatch form) {
        Currency currency = form.getCity().getCurrencyInstance();

        return accountService.createAccountBatch(() -> Account.builder()
                        .withId(UUID.randomUUID())
                        .withCity(form.getCity().getName())
                        .withBalance(Money.zero(currency))
                        .withAllowNegative(false)
                        .withAccountType(AccountType.ASSET)
                        .withName(String.format("user:%05d", monotonicBatchSequence.incrementAndGet()))
                        .withDescription(CockroachFacts.nextFact(256))
                        .build(),
                form.getBatchSize());
    }

    @TransactionImplicit(readOnly = true)
    public Page<Account> findAccounts(AccountType accountType, Pageable pageable) {
        return accountService.findAll(accountType, pageable);
    }

    @TransactionImplicit(readOnly = true)
    public List<Account> findAccounts(List<City> cities, AccountType accountType,
                                      Pair<BigDecimal, BigDecimal> range,
                                      int limit) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting no active transaction");

        List<Callable<List<Account>>> tasks = new ArrayList<>();

        tasks.add(() -> accountService.findByCriteria(City.joinCityNames(cities), accountType, range, limit));

        List<Account> accounts = new ArrayList<>();

        ConcurrencyUtils.runConcurrentlyAndWait(tasks,
                ConcurrencyUtils.UNBOUNDED_CONCURRENCY, accounts::addAll);

        return accounts;
    }

    @TransactionImplicit(readOnly = true)
    public List<Account> findAccounts(City city, AccountType accountType,
                                      Pair<BigDecimal, BigDecimal> range,
                                      int limit) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting no active transaction");

        return accountService.findByCriteria(city.getName(), accountType, range, limit);
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
