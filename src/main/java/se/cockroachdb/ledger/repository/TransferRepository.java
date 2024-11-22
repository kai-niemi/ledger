package se.cockroachdb.ledger.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.domain.TransferType;

public interface TransferRepository {
    Transfer createTransfer(Transfer transfer);

    List<TransferItem> createTransferItems(List<TransferItem> items);

    Transfer findTransferById(UUID transferId);

    boolean checkTransferExists(UUID requestId, String city);

    Page<Transfer> findAllTransfersByAccountId(UUID accountId, Pageable pageable);

    Page<Transfer> findAllTransfersByCity(String city, Pageable pageable);

    Page<Transfer> findAllTransfers(TransferType transferType, Pageable pageable);

    Page<TransferItem> findAllTransferItems(UUID transferId, Pageable pageable);

    void deleteAll();
}
