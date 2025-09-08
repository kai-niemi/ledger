package io.cockroachdb.ledger.repository;

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

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.util.Money;

public interface AccountRepository {
    AccountEntity createAccount(AccountEntity accountEntity);

    List<UUID> createAccounts(Supplier<AccountEntity> factory, int batchSize);

    Optional<AccountEntity> getAccountById(UUID id);

    Money getBalance(UUID id);

    Money getBalanceSnapshot(UUID id);

    void closeAccount(UUID id);

    void openAccount(UUID id);

    void updateBalances(Map<UUID, BigDecimal> balanceUpdates);

    void deleteAll();

    List<AccountEntity> findByCriteria(Set<String> cities,
                                       AccountType accountType,
                                       Pair<BigDecimal, BigDecimal> range,
                                       int limit);

    List<AccountEntity> findById(Set<UUID> ids, boolean forUpdate);

    Page<AccountEntity> findAll(AccountType accountType, Pageable page);
}
