package io.cockroachdb.ledger.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import io.cockroachdb.ledger.annotations.ResponseOutboxEvent;
import io.cockroachdb.ledger.annotations.Retryable;
import io.cockroachdb.ledger.annotations.ServiceFacade;
import io.cockroachdb.ledger.annotations.TransactionExplicit;
import io.cockroachdb.ledger.annotations.TransactionImplicit;
import io.cockroachdb.ledger.annotations.TransactionPriority;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferItemEntity;
import io.cockroachdb.ledger.domain.TransferRequest;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.service.transfer.TransferService;

@ServiceFacade
public class TransferServiceFacade {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransferService transferService;

    @Retryable
    @TransactionExplicit(priority = TransactionPriority.HIGH, retryPriority = TransactionPriority.HIGH)
    @ResponseOutboxEvent(value = TransferEntity.class)
    public TransferEntity createTransfer(TransferRequest request) {
        return transferService.create(request);
    }

    @TransactionImplicit(readOnly = true)
    public Page<TransferEntity> findTransfers(TransferType transferType, @PageableDefault(size = 5) Pageable page) {
        return transferService.findAll(transferType, page);
    }

    @TransactionImplicit(readOnly = true)
    public Page<TransferItemEntity> findTransferItems(UUID id, @PageableDefault(size = 5) Pageable page) {
        return transferService.findAllItems(id, page);
    }
}
