package se.cockroachdb.ledger.repository.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import se.cockroachdb.ledger.domain.TransferEntity;
import se.cockroachdb.ledger.domain.TransferType;

public interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID>,
        JpaSpecificationExecutor<TransferEntity> {
    @Query(value = "select t "
                   + "from TransferEntity t join TransferItemEntity ti on t.id = ti.id.transferId "
                   + "where ti.accountEntity.id = :accountId",
            countQuery = "select count(t.id) "
                         + "from TransferEntity t join TransferItemEntity ti on t.id = ti.id.transferId "
                         + "where ti.accountEntity.id = :accountId")
    Page<TransferEntity> findAll(@Param("accountId") UUID accountId, Pageable pageable);

    @Query(value = "select t "
                   + "from TransferEntity t join TransferItemEntity ti on t.id = ti.id.transferId "
                   + "where ti.accountEntity.city = :city",
            countQuery = "select count(t.id) "
                         + "from TransferEntity t join TransferItemEntity ti on t.id = ti.id.transferId "
                         + "where ti.accountEntity.city = :city")
    Page<TransferEntity> findAllByCity(@Param("city") String city, Pageable pageable);

    @Query(value = "select t "
                   + "from TransferEntity t "
                   + "where t.transferType = :type",
            countQuery = "select count(t.id) "
                         + "from TransferEntity t  "
                         + "where t.transferType = :type")
    Page<TransferEntity> findAll(@Param("type") TransferType transferType, Pageable pageable);
}
