package io.cockroachdb.ledger.it;

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

import io.cockroachdb.ledger.Application;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.account.AccountService;
import io.cockroachdb.ledger.service.transfer.TransferService;
import io.cockroachdb.ledger.util.Money;

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

    protected AccountEntity sekAccountEntity1;

    protected AccountEntity sekAccountEntity2;

    protected AccountEntity sekSystemAccountEntity;

    protected AccountEntity usdAccountEntity1;

    protected AccountEntity usdAccountEntity2;

    protected AccountEntity usdSystemAccountEntity;

    public static final City STH = City.of("stockholm", "swe", "SEK");

    public static final City GTB = City.of("gothenburg", "swe", "SEK");

    public static final City UME = City.of("umeå", "swe", "SEK");

    public static final City NYC = City.of("new york", "usa", "USD");

    public static final City BOS = City.of("boston", "usa", "USD");

    public static final City CHK = City.of("chicago", "usa", "USD");

    protected void createInitialTestAccounts() {
        Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());

        sekAccountEntity1 = accountService.createAccount(
                AccountEntity.builder()
                        .withGeneratedId()
                        .withCity(STH.getName())
                        .withName("test-swe-1")
                        .withAllowNegative(false)
                        .withBalance(Money.of("0.00", "SEK"))
                        .withAccountType(AccountType.ASSET)
                        .withUpdated(LocalDateTime.now()).build());

        sekAccountEntity2 = accountService.createAccount(
                AccountEntity.builder()
                        .withGeneratedId()
                        .withCity(GTB.getName())
                        .withName("test-swe-2")
                        .withBalance(Money.of("0.00", "SEK"))
                        .withAccountType(AccountType.EXPENSE)
                        .withUpdated(LocalDateTime.now()).build());

        sekSystemAccountEntity = accountService.createAccount(
                AccountEntity.builder()
                        .withGeneratedId()
                        .withCity(UME.getName())
                        .withName("test-swe-3")
                        .withAllowNegative(true)
                        .withBalance(Money.of("0.00", "SEK"))
                        .withAccountType(AccountType.LIABILITY)
                        .withUpdated(LocalDateTime.now()).build());

        usdAccountEntity1 = accountService.createAccount(
                AccountEntity.builder()
                        .withGeneratedId()
                        .withCity(NYC.getName())
                        .withName("test-usa-1")
                        .withAllowNegative(false)
                        .withBalance(Money.of("0.00", "USD"))
                        .withAccountType(AccountType.ASSET)
                        .withUpdated(LocalDateTime.now()).build());

        usdAccountEntity2 = accountService.createAccount(
                AccountEntity.builder().withGeneratedId()
                        .withCity(BOS.getName())
                        .withName("test-usa-2")
                        .withBalance(Money.of("0.00", "USD"))
                        .withAccountType(AccountType.EXPENSE)
                        .withUpdated(LocalDateTime.now()).build());

        usdSystemAccountEntity = accountService.createAccount(
                AccountEntity.builder().withGeneratedId()
                        .withCity(CHK.getName())
                        .withName("test-usa-3")
                        .withAllowNegative(true)
                        .withBalance(Money.of("0.00", "USD"))
                        .withAccountType(AccountType.LIABILITY)
                        .withUpdated(LocalDateTime.now()).build());
    }
}
