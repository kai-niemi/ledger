package se.cockroachdb.ledger.it;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.util.Money;

@ActiveProfiles({"default", "integration-test", "jpa"})
@Disabled
public class ImplicitTransactionsTest extends AbstractIntegrationTest {
    @Test
    @Transactional(propagation = Propagation.NEVER)
    @Commit
    public void whenWritingWithoutTransaction_expectFailure() {
        Assertions.assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            accountService.createAccount(
                    Account.builder()
                            .withGeneratedId()
                            .withCity("stockholm")
                            .withName("test-swe-1")
                            .withAllowNegative(false)
                            .withBalance(Money.of("0.00", "SEK"))
                            .withAccountType(AccountType.ASSET)
                            .withUpdated(LocalDateTime.now()).build());
        });
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    public void whenWritingWithTransaction_expectSuccess() {
        accountService.createAccount(
                Account.builder()
                        .withGeneratedId()
                        .withCity("stockholm")
                        .withName("test-swe-1")
                        .withAllowNegative(false)
                        .withBalance(Money.of("0.00", "SEK"))
                        .withAccountType(AccountType.ASSET)
                        .withUpdated(LocalDateTime.now()).build());
    }

    @Test
    @Commit
    public void whenReadingWithoutTransaction_expectSuccess() {
        Page<Account> page = accountService.findAll(Set.of("stockholm"), PageRequest.of(1, 10));
        page.getContent().forEach(account -> {
            System.out.println(account.toString());
        });
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @Commit
    public void whenWritingWithTransactionTemplate_expectSuccess() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            accountService.createAccount(
                    Account.builder()
                            .withGeneratedId()
                            .withCity("stockholm")
                            .withName("test-swe-1")
                            .withAllowNegative(false)
                            .withBalance(Money.of("0.00", "SEK"))
                            .withAccountType(AccountType.ASSET)
                            .withUpdated(LocalDateTime.now()).build());
        });
    }
}
