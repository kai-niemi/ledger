package se.cockroachdb.ledger.it;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.boot.ShellRunnerAutoConfiguration;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import se.cockroachdb.ledger.Application;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.service.account.AccountService;
import se.cockroachdb.ledger.service.transfer.TransferService;
import se.cockroachdb.ledger.util.Money;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        ShellRunnerAutoConfiguration.class // disable shell for it
})
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("integration-test") // short "it"
public abstract class AbstractIntegrationTest {
    @Autowired
    protected TransferService transferService;

    @Autowired
    protected AccountService accountService;

    protected Account sekAccount1;

    protected Account sekAccount2;

    protected Account sekSystemAccount;

    protected Account usdAccount1;

    protected Account usdAccount2;

    protected Account usdSystemAccount;

    protected void createInitialTestAccounts() {
        Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());

        sekAccount1 = accountService.createAccount(
                        Account.builder()
                                .withGeneratedId()
                                .withCity("stockholm")
                                .withName("test-swe-1")
                                .withAllowNegative(false)
                                .withBalance(Money.of("0.00", "SEK"))
                                .withAccountType(AccountType.ASSET)
                                .withUpdated(LocalDateTime.now()).build());

        sekAccount2 = accountService.createAccount(
                        Account.builder()
                                .withGeneratedId()
                                .withCity("stockholm")
                                .withName("test-swe-2")
                                .withBalance(Money.of("0.00", "SEK"))
                                .withAccountType(AccountType.EXPENSE)
                                .withUpdated(LocalDateTime.now()).build());

        sekSystemAccount = accountService.createAccount(
                        Account.builder()
                                .withGeneratedId()
                                .withCity("gothenburg")
                                .withName("test-swe-3")
                                .withAllowNegative(true)
                                .withBalance(Money.of("0.00", "SEK"))
                                .withAccountType(AccountType.LIABILITY)
                                .withUpdated(LocalDateTime.now()).build());

        usdAccount1 = accountService.createAccount(
                        Account.builder()
                                .withGeneratedId()
                                .withCity("new york")
                                .withName("test-usa-1")
                                .withAllowNegative(false)
                                .withBalance(Money.of("0.00", "USD"))
                                .withAccountType(AccountType.ASSET)
                                .withUpdated(LocalDateTime.now()).build());

        usdAccount2 = accountService.createAccount(
                        Account.builder().withGeneratedId()
                                .withCity("new york")
                                .withName("test-usa-2")
                                .withBalance(Money.of("0.00", "USD"))
                                .withAccountType(AccountType.EXPENSE)
                                .withUpdated(LocalDateTime.now()).build());

        usdSystemAccount = accountService.createAccount(
                        Account.builder().withGeneratedId()
                                .withCity("chicago")
                                .withName("test-usa-3")
                                .withAllowNegative(true)
                                .withBalance(Money.of("0.00", "USD"))
                                .withAccountType(AccountType.LIABILITY)
                                .withUpdated(LocalDateTime.now()).build());
    }
}
