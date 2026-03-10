package io.cockroachdb.ledger.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import io.cockroachdb.ledger.annotation.Retryable;
import io.cockroachdb.ledger.annotation.TransactionExplicit;


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
}

