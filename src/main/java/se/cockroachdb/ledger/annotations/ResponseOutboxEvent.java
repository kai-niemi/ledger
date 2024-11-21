package se.cockroachdb.ledger.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for business service methods that
 * return DTOs qualifying as outbox events.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ResponseOutboxEvent {
    Class<? extends EventAggregate<?>> value();
}
