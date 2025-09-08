package io.cockroachdb.ledger.service.transfer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferItemEntity;
import io.cockroachdb.ledger.domain.TransferRequest;
import io.cockroachdb.ledger.domain.TransferType;

public interface TransferService {
    TransferEntity create(TransferRequest transferRequest);

    TransferEntity findById(UUID id);

    Page<TransferEntity> findAll(TransferType transferType, Pageable page);

    Page<TransferEntity> findAllByAccountId(UUID accountId, Pageable page);

    Page<TransferEntity> findAllByCity(String city, Pageable page);

    Page<TransferItemEntity> findAllItems(UUID transferId, Pageable page);

    void deleteAll();
}
