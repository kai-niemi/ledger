package se.cockroachdb.ledger.service.transfer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import se.cockroachdb.ledger.domain.TransferEntity;
import se.cockroachdb.ledger.domain.TransferItemEntity;
import se.cockroachdb.ledger.domain.TransferRequest;
import se.cockroachdb.ledger.domain.TransferType;

public interface TransferService {
    TransferEntity createTransfer(TransferRequest transferRequest);

    TransferEntity findById(UUID id);

    Page<TransferEntity> findAll(TransferType transferType, Pageable page);

    Page<TransferEntity> findAllByAccountId(UUID accountId, Pageable page);

    Page<TransferEntity> findAllByCity(String city, Pageable page);

    Page<TransferItemEntity> findAllItems(UUID transferId, Pageable page);

    void deleteAll();
}
