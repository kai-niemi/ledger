package se.cockroachdb.ledger.service.workload;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Background workload task with a completion predicate.
 */
public interface Worker<T> extends Callable<T>, Predicate<Integer> {
}
