package io.cockroachdb.ledger.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import io.cockroachdb.ledger.Application;
import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.aspect.AdvisorOrder;

@Configuration
@EnableTransactionManagement(order = AdvisorOrder.TRANSACTION_MANAGER_ADVISOR, proxyTargetClass = true)
@EnableJpaRepositories(basePackageClasses = {Application.class}, enableDefaultTransactions = false)
@Profile(ProfileNames.JPA)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JpaTransactionConfig {
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(platformTransactionManager);
    }
}

