package se.cockroachdb.ledger.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.aspect.ExponentialBackoffRetryHandler;
import se.cockroachdb.ledger.aspect.OutboxAspect;
import se.cockroachdb.ledger.aspect.RetryHandler;
import se.cockroachdb.ledger.aspect.TransactionDecoratorAspect;
import se.cockroachdb.ledger.aspect.TransactionRetryAspect;
import se.cockroachdb.ledger.repository.OutboxRepository;
import se.cockroachdb.ledger.repository.jdbc.JdbcOutboxRepository;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AspectConfig {
    @Bean
    @Profile(ProfileNames.OUTBOX)
    public OutboxAspect outboxAspect() {
        return new OutboxAspect(outboxRepository());
    }

    @Bean
    @Profile(ProfileNames.OUTBOX)
    public OutboxRepository outboxRepository() {
        return new JdbcOutboxRepository();
    }

    @Bean
    @Profile(ProfileNames.RETRIES)
    public TransactionRetryAspect transactionRetryAspect() {
        return new TransactionRetryAspect(retryHandler());
    }

    @Bean
    @Profile(ProfileNames.RETRIES)
    public RetryHandler retryHandler() {
        return new ExponentialBackoffRetryHandler();
    }

    @Bean
    @Profile("!psql")
    public TransactionDecoratorAspect transactionDecoratorAspect(@Autowired DataSource dataSource) {
        return new TransactionDecoratorAspect(dataSource);
    }
}
