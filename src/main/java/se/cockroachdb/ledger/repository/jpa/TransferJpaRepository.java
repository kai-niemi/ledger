package se.cockroachdb.ledger.repository.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import se.cockroachdb.ledger.domain.Transfer;

public interface TransferJpaRepository extends JpaRepository<Transfer, UUID>,
        JpaSpecificationExecutor<Transfer> {
    @Query(value = "select t "
                   + "from Transfer t join TransferItem ti on t.id = ti.id.transferId "
                   + "where ti.account.id = ?1",
            countQuery = "select count(t.id) "
                         + "from Transfer t join TransferItem ti on t.id = ti.id.transferId "
                         + "where ti.account.id = ?1")
    Page<Transfer> findAll(@Param("accountId") UUID accountId, Pageable pageable);

    @Query(value = "select t "
                   + "from Transfer t join TransferItem ti on t.id = ti.id.transferId "
                   + "where ti.account.city = ?1",
            countQuery = "select count(t.id) "
                         + "from Transfer t join TransferItem ti on t.id = ti.id.transferId "
                         + "where ti.account.city = ?1")
    Page<Transfer> findAllByCity(@Param("city") String city, Pageable pageable);
}
