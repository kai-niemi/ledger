package se.cockroachdb.ledger.service.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.util.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public interface AccountService {
    Account createAccount(Account account);

    List<UUID> createAccountBatch(Supplier<Account> factory, int batchSize);

    List<Account> findByCriteria(Set<String> cities, AccountType accountType, int limit);

    List<Account> findByCriteria(String city,
                                 AccountType accountType,
                                 Pair<BigDecimal, BigDecimal> range,
                                 int limit);

    Page<Account> findAll(AccountType accountType, Pageable page);

    Page<Account> findAll(Set<String> cities, Pageable page);

    Account findById(UUID id);

    Money getBalance(UUID id);

    Money getBalanceSnapshot(UUID id);

    Account openAccount(UUID id);

    Account closeAccount(UUID id);

    void deleteAll();
}
