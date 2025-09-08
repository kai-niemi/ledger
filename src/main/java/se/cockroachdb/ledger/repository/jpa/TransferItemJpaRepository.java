package se.cockroachdb.ledger.repository.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import se.cockroachdb.ledger.domain.TransferItemEntity;
import se.cockroachdb.ledger.domain.TransferItemId;

public interface TransferItemJpaRepository extends JpaRepository<TransferItemEntity, TransferItemId>,
        JpaSpecificationExecutor<TransferItemEntity> {

    @Query(value
            = "select item from TransferItemEntity item "
              + "where item.transferEntity.id = :transferId",
            countQuery
                    = "select count(item.id.transferId) from TransferItemEntity item "
                      + "where item.transferEntity.id = :transferId")
    Page<TransferItemEntity> findById(
            @Param("transferId") UUID transferId,
            Pageable page);
}
