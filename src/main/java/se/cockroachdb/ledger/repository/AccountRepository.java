package se.cockroachdb.ledger.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.util.Money;

public interface AccountRepository {
    Account createAccount(Account account);

    List<UUID> createAccounts(Supplier<Account> factory, int batchSize);

    Optional<Account> getAccountById(UUID id);

    Money getBalance(UUID id);

    Money getBalanceSnapshot(UUID id);

    void closeAccount(UUID id);

    void openAccount(UUID id);

    void updateBalances(Map<UUID, Pair<String, BigDecimal>> accountUpdates);

    void deleteAll();

    List<Account> findByCriteria(Set<String> cities, AccountType accountType, int limit);

    List<Account> findByCriteria(String city, AccountType accountType, Pair<BigDecimal, BigDecimal> range,
                                 int limit);

    List<Account> findById(Set<String> cities, Set<UUID> ids, boolean forUpdate);

    Page<Account> findAll(AccountType accountType, Pageable page);

    Page<Account> findAll(Set<String> cities, Pageable page);
}
