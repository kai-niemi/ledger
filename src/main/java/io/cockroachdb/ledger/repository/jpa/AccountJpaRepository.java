package io.cockroachdb.ledger.repository.jpa;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.util.Money;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID>,
        JpaSpecificationExecutor<AccountEntity> {

    @Query(value = "select a.balance "
                   + "from AccountEntity a "
                   + "where a.id = ?1")
    Money findBalanceById(UUID id);

    @Query(value = "select "
                   + "count (a.id), "
                   + "count (distinct a.city), "
                   + "sum (a.balance.amount), "
                   + "min (a.balance.amount), "
                   + "max (a.balance.amount), "
                   + "max(a.updatedAt), "
                   + "a.balance.currency "
                   + "from AccountEntity a "
                   + "where a.city = ?1 "
                   + "group by a.city,a.balance.currency")
    List<Tuple> accountSummary(String city);

    @Query(value = "select "
                   + "  count (distinct t.id), "
                   + "  count (t.id), "
                   + "  sum (abs(ti.amount.amount)), "
                   + "  sum (ti.amount.amount), "
                   + "  ti.amount.currency "
                   + "from TransferEntity t join TransferItemEntity ti on t.id = ti.id.transferId "
                   + "where ti.city = ?1 "
                   + "group by ti.city, ti.amount.currency")
    List<Tuple> transactionSummary(String city);

    @Query(value = "select a "
                   + "from AccountEntity a "
                   + "where a.id in (?1)")
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AccountEntity> findAllWithLock(Set<UUID> ids);

    @Query(value = "select a "
                   + "from AccountEntity a "
                   + "where a.id in (?1)")
    List<AccountEntity> findAll(Set<UUID> ids);

    @Query(value
            = "select a "
              + "from AccountEntity a "
              + "where a.city in (:cities)",
            countQuery
                    = "select count(a.id) "
                      + "from AccountEntity a "
                      + "where a.city in (:cities)")
    Page<AccountEntity> findAll(@Param("cities") Collection<String> cities, Pageable pageable);

    @Query(value
            = "select a "
              + "from AccountEntity a "
              + "where a.city in (:cities) and a.accountType = :type",
            countQuery
                    = "select count(a.id) "
                      + "from AccountEntity a "
                      + "where a.city in (:cities) and a.accountType = :type")
    Page<AccountEntity> findAll(@Param("cities") Collection<String> cities,
                                @Param("type") AccountType accountType,
                                Pageable pageable);

    @Query(value
            = "select a "
              + "from AccountEntity a "
              + "where a.city in (:cities) and a.accountType = :type "
              + "and a.balance.amount between :min and :max",
            countQuery
                    = "select count(a.id) "
                      + "from AccountEntity a "
                      + "where a.city in (:cities) and a.accountType = :type "
                      + "and a.balance.amount between :min and :max")
    Page<AccountEntity> findAll(@Param("cities") Collection<String> cities,
                                @Param("type") AccountType accountType,
                                @Param("min") BigDecimal min,
                                @Param("max") BigDecimal max,
                                Pageable pageable);

    @Query(value
            = "select a "
              + "from AccountEntity a "
              + "where a.city = :city "
              + "and a.accountType = :type "
              + "and a.balance.amount between :min and :max",
            countQuery
                    = "select count(a.id) "
                      + "from AccountEntity a "
                      + "where a.city = :city "
                      + "and a.accountType = :type "
                      + "and a.balance.amount between :min and :max")
    Page<AccountEntity> findAll(@Param("city") String city,
                                @Param("type") AccountType accountType,
                                @Param("min") BigDecimal min,
                                @Param("max") BigDecimal max,
                                Pageable pageable);

    @Query(value
            = "select a "
              + "from AccountEntity a "
              + "where a.accountType = :type",
            countQuery
                    = "select count(a.id) "
                      + "from AccountEntity a "
                      + "where a.accountType = :type")
    Page<AccountEntity> findAll(@Param("type") AccountType type, Pageable pageable);
}
