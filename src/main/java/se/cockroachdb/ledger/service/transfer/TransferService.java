package se.cockroachdb.ledger.service.transfer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.model.TransferRequest;

public interface TransferService {
    Transfer createTransfer(TransferRequest transferRequest);

    Transfer findById(UUID id);

    Page<Transfer> findAll(TransferType transferType, Pageable page);

    Page<Transfer> findAllByAccountId(UUID accountId, Pageable page);

    Page<Transfer> findAllByCity(String city, Pageable page);

    Page<TransferItem> findAllItems(UUID transferId, Pageable page);

    void deleteAll();
}
