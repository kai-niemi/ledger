package se.cockroachdb.ledger.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import se.cockroachdb.ledger.annotations.ResponseOutboxEvent;
import se.cockroachdb.ledger.annotations.Retryable;
import se.cockroachdb.ledger.annotations.ServiceFacade;
import se.cockroachdb.ledger.annotations.TransactionExplicit;
import se.cockroachdb.ledger.annotations.TransactionImplicit;
import se.cockroachdb.ledger.annotations.TransactionPriority;
import se.cockroachdb.ledger.domain.TransferEntity;
import se.cockroachdb.ledger.domain.TransferItemEntity;
import se.cockroachdb.ledger.domain.TransferRequest;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.service.transfer.TransferService;

@ServiceFacade
public class TransferServiceFacade {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransferService transferService;

    @Retryable
    @TransactionExplicit(priority = TransactionPriority.HIGH, retryPriority = TransactionPriority.HIGH)
    @ResponseOutboxEvent(value = TransferEntity.class)
    public TransferEntity createTransfer(TransferRequest request) {
        return transferService.createTransfer(request);
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
