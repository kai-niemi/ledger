package se.cockroachdb.ledger.service.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import se.cockroachdb.ledger.domain.AccountEntity;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.util.Money;

public interface AccountService {
    AccountEntity createAccount(AccountEntity accountEntity);

    List<UUID> createAccountBatch(Supplier<AccountEntity> factory, int batchSize);

    Page<AccountEntity> findAll(AccountType accountType, Pageable page);

    List<AccountEntity> findByCriteria(Set<String> cities,
                                       AccountType accountType,
                                       Pair<BigDecimal, BigDecimal> range,
                                       int limit);

    AccountEntity findById(UUID id);

    Money getBalance(UUID id);

    Money getBalanceSnapshot(UUID id);

    AccountEntity openAccount(UUID id);

    AccountEntity closeAccount(UUID id);

    void deleteAll();
}
