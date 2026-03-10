package io.cockroachdb.ledger.service.transfer;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import io.cockroachdb.ledger.annotation.AdvisorOrder;
import io.cockroachdb.ledger.annotation.ResponseOutboxEvent;
import io.cockroachdb.ledger.repository.OutboxRepository;

@Aspect
@Order(OutboxAspect.PRECEDENCE)
public class OutboxAspect {
    public static final int PRECEDENCE = AdvisorOrder.CHANGE_FEED_ADVISOR;

    private final OutboxRepository outboxRepository;

    public OutboxAspect(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Pointcut expression matching all outbox event operations.
     */
    @Pointcut("execution(public * *(..)) "
              + "&& @annotation(outboxPayload)")
    public void anyOutboxEventOperation(ResponseOutboxEvent outboxPayload) {
    }

    @AfterReturning(pointcut = "anyOutboxEventOperation(responseOutboxEvent)",
            returning = "returnValue", argNames = "returnValue,responseOutboxEvent")
    public void doAfterOutboxOperation(Object returnValue, ResponseOutboxEvent responseOutboxEvent) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected existing transaction - check advisor @Order");
        outboxRepository.writeEvent(responseOutboxEvent.value().cast(returnValue));
    }
}

