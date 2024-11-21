package se.cockroachdb.ledger.repository.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.domain.TransferItemId;

public interface TransferItemJpaRepository extends JpaRepository<TransferItem, TransferItemId>,
        JpaSpecificationExecutor<TransferItem> {

    @Query(value
            = "select item from TransferItem item "
              + "where item.transfer.id = :transferId",
            countQuery
                    = "select count(item.id.transferId) from TransferItem item "
                      + "where item.transfer.id = :transferId")
    Page<TransferItem> findById(
            @Param("transferId") UUID transferId,
            Pageable page);
}
