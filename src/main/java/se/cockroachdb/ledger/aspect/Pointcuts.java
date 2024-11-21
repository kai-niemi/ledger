package se.cockroachdb.ledger.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import se.cockroachdb.ledger.annotations.ResponseOutboxEvent;
import se.cockroachdb.ledger.annotations.Retryable;
import se.cockroachdb.ledger.annotations.TransactionExplicit;


@Aspect
public class Pointcuts {
    /**
     * Pointcut expression matching all transactional boundaries.
     */
    @Pointcut("execution(public * *(..)) "
            + "&& @annotation(transactionExplicit)")
    public void anyTransactionBoundaryOperation(TransactionExplicit transactionExplicit) {
    }

    /**
     * Pointcut expression matching all retryable operations.
     */
    @Pointcut("execution(public * *(..)) "
            + "&& @annotation(retryable)")
    public void anyRetryableOperation(Retryable retryable) {
    }

    /**
     * Pointcut expression matching all outbox event operations.
     */
    @Pointcut("execution(public * *(..)) "
              + "&& @annotation(outboxPayload)")
    public void anyOutboxEventOperation(ResponseOutboxEvent outboxPayload) {
    }
}

