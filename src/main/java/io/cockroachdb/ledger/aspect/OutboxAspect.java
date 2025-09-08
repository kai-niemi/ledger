package io.cockroachdb.ledger.aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import io.cockroachdb.ledger.annotations.ResponseOutboxEvent;
import io.cockroachdb.ledger.repository.OutboxRepository;

@Aspect
@Order(OutboxAspect.PRECEDENCE)
public class OutboxAspect {
    public static final int PRECEDENCE = AdvisorOrder.CHANGE_FEED_ADVISOR;

    private final OutboxRepository outboxRepository;

    public OutboxAspect(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @AfterReturning(pointcut = "io.cockroachdb.ledger.aspect.Pointcuts.anyOutboxEventOperation(responseOutboxEvent)",
            returning = "returnValue", argNames = "returnValue,responseOutboxEvent")
    public void doAfterOutboxOperation(Object returnValue, ResponseOutboxEvent responseOutboxEvent) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected existing transaction - check advisor @Order");
        outboxRepository.writeEvent(responseOutboxEvent.value().cast(returnValue));
    }
}

