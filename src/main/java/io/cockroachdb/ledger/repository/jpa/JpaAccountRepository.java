package io.cockroachdb.ledger.repository.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.repository.AccountRepository;
import io.cockroachdb.ledger.util.Money;

@Repository
@Profile(ProfileNames.JPA)
public class JpaAccountRepository implements AccountRepository {
    @Autowired
    private AccountJpaRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Environment environment;

    @Override
    public List<UUID> createAccounts(Supplier<AccountEntity> factory, int batchSize) {
        List<UUID> ids = new ArrayList<>();
        IntStream.rangeClosed(1, batchSize).forEach(value -> {
            AccountEntity accountEntity = factory.get();
            accountRepository.save(accountEntity);
            ids.add(accountEntity.getId());
        });
        return ids;
    }

    @Override
    public AccountEntity createAccount(AccountEntity accountEntity) {
        return accountRepository.save(accountEntity);
    }

    @Override
    public void updateBalances(Map<UUID, BigDecimal> balanceUpdates) {
        balanceUpdates.forEach((uuid, amount) -> {
            Query q = entityManager.createQuery("UPDATE AccountEntity a"
                                                + " SET"
                                                + "   a.balance.amount = a.balance.amount + ?2,"
                                                + "   a.updatedAt = ?3"
                                                + " WHERE a.id = ?1"
                                                + "   AND (a.balance.amount + ?2) * abs(a.allowNegative - 1) >= 0");

            q.setParameter(1, uuid);
            q.setParameter(2, amount);
            q.setParameter(3, LocalDateTime.now());

            int rows = q.executeUpdate();

            if (rows != 1) {
                throw new IncorrectResultSizeDataAccessException(1, rows);
            }
        });
    }

    @Override
    public void closeAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.getReferenceById(id);
        if (!accountEntity.isClosed()) {
            accountEntity.setClosed(true);
        }
    }

    @Override
    public void openAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.getReferenceById(id);
        if (accountEntity.isClosed()) {
            accountEntity.setClosed(false);
        }
    }

    @Override
    public Optional<AccountEntity> getAccountById(UUID id) {
        return accountRepository.findById(id);
    }

    @Override
    public Money getBalance(UUID id) {
        return accountRepository.findBalanceById(id);
    }

    @Override
    public Money getBalanceSnapshot(UUID id) {
        if (ProfileNames.acceptsPostgresSQL(environment)) {
            return getBalance(id);
        }
        Query q = entityManager.createNativeQuery("select a.currency,a.balance " +
                                                  "from account a " +
                                                  "as of system time follower_read_timestamp() " +
                                                  "where a.id = ?1", Tuple.class);
        q.setParameter(1, id);
        Tuple tuple = (Tuple) q.getSingleResult();
        return Money.of(
                tuple.get(1, BigDecimal.class).toPlainString(),
                tuple.get(0, String.class));
    }

    @Override
    public List<AccountEntity> findById(Set<UUID> ids, boolean forUpdate) {
        return forUpdate
                ? accountRepository.findAllWithLock(ids)
                : accountRepository.findAll(ids);
    }

    @Override
    public List<AccountEntity> findByCriteria(Set<String> cities, AccountType accountType,
                                              Pair<BigDecimal, BigDecimal> range,
                                              int limit) {
        List<AccountEntity> accountEntityEntities = new ArrayList<>();

        // No window functions in JPA :o

        // Equality cancels out balance range filtering
        if (range.getFirst().equals(range.getSecond())) {
            cities.forEach(c -> accountEntityEntities.addAll(
                    accountRepository.findAll(List.of(c), accountType,
                            PageRequest.ofSize(limit)).getContent()));
        } else {
            cities.forEach(c -> accountEntityEntities.addAll(
                    accountRepository.findAll(List.of(c), accountType,
                            range.getFirst(), range.getSecond(),
                            PageRequest.ofSize(limit)).getContent()));
        }
        return accountEntityEntities;
    }

    @Override
    public void deleteAll() {
        accountRepository.deleteAllInBatch();
    }

    @Override
    public Page<AccountEntity> findAll(AccountType accountType, Pageable page) {
        return accountRepository.findAll(accountType, page);
    }
}
