package io.cockroachdb.ledger.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.support.TransactionTemplate;

import io.cockroachdb.ledger.Application;
import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.aspect.AdvisorOrder;

@Configuration
@EnableTransactionManagement(order = AdvisorOrder.TRANSACTION_MANAGER_ADVISOR, proxyTargetClass = true)
@EnableJdbcRepositories(basePackageClasses = {Application.class})
@Profile(ProfileNames.NOT_JPA)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JdbcTransactionConfig implements TransactionManagementConfigurer {
    @Autowired
    private DataSource dataSource;

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        transactionManager.setEnforceReadOnly(true);
        transactionManager.setNestedTransactionAllowed(true);
        transactionManager.setRollbackOnCommitFailure(false);
        transactionManager.setDefaultTimeout(-1);
        return transactionManager;
    }

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
