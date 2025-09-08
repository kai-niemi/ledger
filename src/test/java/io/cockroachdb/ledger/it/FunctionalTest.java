package io.cockroachdb.ledger.it;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import io.cockroachdb.ledger.annotations.TransactionExplicit;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.domain.TransferRequest;
import io.cockroachdb.ledger.service.BadRequestException;
import io.cockroachdb.ledger.service.NegativeBalanceException;
import io.cockroachdb.ledger.util.Money;
import static io.cockroachdb.ledger.util.Money.SEK;
import static io.cockroachdb.ledger.util.Money.USD;

@ActiveProfiles({"default", "integration-test"})
public class FunctionalTest extends AbstractIntegrationTest {
    @Test
    @Order(0)
    @TransactionExplicit
    @Commit
    public void whenStartingTest_expectInitialAccountsCreated() {
        createInitialTestAccounts();
    }

    @Test
    @Order(1)
    @TransactionExplicit
    @Commit
    public void givenExistingAccounts_whenReadingBalances_thenInitialSums() {
        {
            AccountEntity accountEntity = accountService.findById(sekAccountEntity1.getId());
            Assertions.assertEquals(0, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), accountEntity.getBalance());
            Assertions.assertEquals("test-swe-1", accountEntity.getName());
        }

        {
            AccountEntity accountEntity = accountService.findById(sekAccountEntity2.getId());
            Assertions.assertEquals(0, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), accountEntity.getBalance());
            Assertions.assertEquals("test-swe-2", accountEntity.getName());
        }

        {
            AccountEntity accountEntity = accountService.findById(sekSystemAccountEntity.getId());
            Assertions.assertEquals(1, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), accountEntity.getBalance());
            Assertions.assertEquals("test-swe-3", accountEntity.getName());
        }

        {
            AccountEntity accountEntity = accountService.findById(usdAccountEntity1.getId());
            Assertions.assertEquals(0, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), accountEntity.getBalance());
            Assertions.assertEquals("test-usa-1", accountEntity.getName());
        }

        {
            AccountEntity accountEntity = accountService.findById(usdAccountEntity2.getId());
            Assertions.assertEquals(0, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), accountEntity.getBalance());
            Assertions.assertEquals("test-usa-2", accountEntity.getName());
        }

        {
            AccountEntity accountEntity = accountService.findById(usdSystemAccountEntity.getId());
            Assertions.assertEquals(1, accountEntity.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), accountEntity.getBalance());
            Assertions.assertEquals("test-usa-3", accountEntity.getName());
        }
    }

    @Test
    @Order(2)
    @TransactionExplicit
    @Commit
    public void givenInitialSEKAccountBalances_whenIssuingBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        AccountEntity accountEntityA = accountService.findById(sekAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(sekAccountEntity2.getId());
        AccountEntity accountEntityC = accountService.findById(sekSystemAccountEntity.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);
        Assertions.assertNotEquals(accountEntityA, accountEntityC);
        Assertions.assertNotEquals(accountEntityB, accountEntityC);

        TransferRequest request = TransferRequest.builder()
                .withTransferType(TransferType.BANK)
                .withId(UUID.randomUUID())
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("500.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("250.00", accountEntityB.getBalance().getCurrency()))
                .withNote("credit B")
                .then()
                .addItem()
                .withId(accountEntityC.getId())
                .withAmount(Money.of("-750.00", accountEntityC.getBalance().getCurrency()))
                .withNote("debit A+B")
                .then()
                .build();

        TransferEntity transferEntity = transferService.create(request);
        Assertions.assertNotNull(transferEntity);
        Assertions.assertEquals(3, transferEntity.getItems().size());
        Assertions.assertEquals(TransferType.BANK, transferEntity.getTransferType());
        Assertions.assertTrue(transferEntity.isNew());
    }

    @Test
    @Order(3)
    @TransactionExplicit
    @Commit
    public void givenInitialUSDAccountBalances_whenIssuingBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        AccountEntity accountEntityA = accountService.findById(usdAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(usdAccountEntity2.getId());
        AccountEntity accountEntityC = accountService.findById(usdSystemAccountEntity.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);
        Assertions.assertNotEquals(accountEntityA, accountEntityC);
        Assertions.assertNotEquals(accountEntityB, accountEntityC);

        TransferRequest request = TransferRequest.builder()
                .withTransferType(TransferType.BANK)
                .withId(UUID.randomUUID())
                .withCity(NYC)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("500.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("250.00", accountEntityB.getBalance().getCurrency()))
                .withNote("credit B")
                .then()
                .addItem()
                .withId(accountEntityC.getId())
                .withAmount(Money.of("-750.00", accountEntityC.getBalance().getCurrency()))
                .withNote("debit A+B")
                .then()
                .build();

        TransferEntity transferEntity = transferService.create(request);
        Assertions.assertNotNull(transferEntity);
        Assertions.assertEquals(3, transferEntity.getItems().size());
        Assertions.assertEquals(TransferType.BANK, transferEntity.getTransferType());
        Assertions.assertTrue(transferEntity.isNew());
    }

    @Test
    @Order(4)
    @TransactionExplicit
    @Commit
    public void givenCurrentAccountBalances_whenIssuingOneBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        AccountEntity accountEntityA = accountService.findById(sekAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(sekAccountEntity2.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);

        TransferRequest request = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("-50.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("50.00", accountEntityB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        TransferEntity transferEntity = transferService.create(request);
        Assertions.assertNotNull(transferEntity);
        Assertions.assertEquals(2, transferEntity.getItems().size());
        Assertions.assertEquals(TransferType.PAYMENT, transferEntity.getTransferType());
        Assertions.assertTrue(transferEntity.isNew());
    }

    private void assertZeroOrPositive(Money money, String addend) {
        Money newMoney = money.plus(Money.of(addend, money.getCurrency()));
        Assertions.assertFalse(newMoney.isNegative(), newMoney + " is negative");
    }

    @Test
    @Order(5)
    @TransactionExplicit
    @Commit
    public void givenCurrentAccountBalances_whenIssuingMultiLeggedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        AccountEntity sekFrom = accountService.findById(sekAccountEntity1.getId());
        AccountEntity sekTo = accountService.findById(sekAccountEntity2.getId());
        AccountEntity usdFrom = accountService.findById(usdAccountEntity1.getId());
        AccountEntity usdTo = accountService.findById(usdAccountEntity2.getId());

        Assertions.assertNotEquals(sekFrom, sekTo);
        Assertions.assertNotEquals(usdFrom, usdTo);
        Assertions.assertNotEquals(usdFrom, sekTo);
        Assertions.assertNotEquals(usdFrom, sekFrom);

        assertZeroOrPositive(sekFrom.getBalance(), "-150.00");
        assertZeroOrPositive(usdFrom.getBalance(), "-251.00");

        TransferRequest request = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .withTransferType(TransferType.PAYMENT)
                .addItem()
                .withId(sekFrom.getId())
                .withAmount(Money.of("-50.00", sekFrom.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(sekFrom.getId())
                .withAmount(Money.of("-100.00", sekFrom.getBalance().getCurrency()))
                .withNote("debit B")
                .then()
                .addItem()
                .withId(sekTo.getId())
                .withAmount(Money.of("150.00", sekTo.getBalance().getCurrency()))
                .withNote("credit A+B")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withAmount(Money.of("-250.05", usdFrom.getBalance().getCurrency()))
                .withNote("debit C")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withAmount(Money.of("-0.55", usdFrom.getBalance().getCurrency()))
                .withNote("debit D.1")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withAmount(Money.of("0.55", usdFrom.getBalance().getCurrency()))
                .withNote("debit D.2 - revert D.1")
                .then()
                .addItem()
                .withId(usdTo.getId())
                .withAmount(Money.of("250.05", usdTo.getBalance().getCurrency()))
                .withNote("credit C+D")
                .then()
                .build();

        TransferEntity transferEntity = transferService.create(request);
        Assertions.assertNotNull(transferEntity);
        Assertions.assertEquals(4, transferEntity.getItems().size());
        Assertions.assertEquals(TransferType.PAYMENT, transferEntity.getTransferType());
        Assertions.assertTrue(transferEntity.isNew());
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(6)
    public void givenCurrentAccountBalances_whenIssuingUnbalancedFundsTransfer_thenFail() {
        AccountEntity accountEntityA = accountService.findById(sekAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(sekAccountEntity2.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("-50.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("50.05", accountEntityB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(BadRequestException.class, () -> {
            transferService.create(form);
        });
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(7)
    public void givenCurrentAccountBalances_whenIssuingUnbalancedMultiCurrencyFundsTransfer_thenFail() {
        AccountEntity accountEntityA = accountService.findById(sekAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(usdAccountEntity1.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("-50.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("50.00", "USD"))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(BadRequestException.class, () -> {
            transferService.create(form);
        });
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(8)
    public void givenCurrentAccountBalances_whenIssuingFundsTransferWithNegativeBalance_thenFail() {
        AccountEntity accountEntityA = accountService.findById(sekAccountEntity1.getId());
        AccountEntity accountEntityB = accountService.findById(sekAccountEntity2.getId());

        Assertions.assertNotEquals(accountEntityA, accountEntityB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity(STH)
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountEntityA.getId())
                .withAmount(Money.of("-5000.00", accountEntityA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountEntityB.getId())
                .withAmount(Money.of("5000.00", accountEntityB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(NegativeBalanceException.class, () -> {
            transferService.create(form);
        });
    }

}
