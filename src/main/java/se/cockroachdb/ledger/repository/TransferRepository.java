package se.cockroachdb.ledger.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import se.cockroachdb.ledger.domain.TransferEntity;
import se.cockroachdb.ledger.domain.TransferItemEntity;
import se.cockroachdb.ledger.domain.TransferType;

public interface TransferRepository {
    TransferEntity createTransfer(TransferEntity transferEntity);

    List<TransferItemEntity> createTransferItems(List<TransferItemEntity> items);

    TransferEntity findTransferById(UUID transferId);

    boolean checkTransferExists(UUID requestId);

    Page<TransferEntity> findAllTransfersByAccountId(UUID accountId, Pageable pageable);

    Page<TransferEntity> findAllTransfersByCity(String city, Pageable pageable);

    Page<TransferEntity> findAllTransfers(TransferType transferType, Pageable pageable);

    Page<TransferItemEntity> findAllTransferItems(UUID transferId, Pageable pageable);

    void deleteAll();
}
