package se.cockroachdb.ledger.it;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import se.cockroachdb.ledger.annotations.TransactionExplicit;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.model.TransferRequest;
import se.cockroachdb.ledger.service.BadRequestException;
import se.cockroachdb.ledger.service.NegativeBalanceException;
import se.cockroachdb.ledger.util.Money;
import static se.cockroachdb.ledger.util.Money.SEK;
import static se.cockroachdb.ledger.util.Money.USD;

@ActiveProfiles({"default", "integration-test", "jpa"})
//@ActiveProfiles({"default", "integration-test"})
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
            Account account = accountService.findById(sekAccount1.getId());
            Assertions.assertEquals(0, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), account.getBalance());
            Assertions.assertEquals("test-swe-1", account.getName());
        }

        {
            Account account = accountService.findById(sekAccount2.getId());
            Assertions.assertEquals(0, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), account.getBalance());
            Assertions.assertEquals("test-swe-2", account.getName());
        }

        {
            Account account = accountService.findById(sekSystemAccount.getId());
            Assertions.assertEquals(1, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", SEK), account.getBalance());
            Assertions.assertEquals("test-swe-3", account.getName());
        }

        {
            Account account = accountService.findById(usdAccount1.getId());
            Assertions.assertEquals(0, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), account.getBalance());
            Assertions.assertEquals("test-usa-1", account.getName());
        }

        {
            Account account = accountService.findById(usdAccount2.getId());
            Assertions.assertEquals(0, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), account.getBalance());
            Assertions.assertEquals("test-usa-2", account.getName());
        }

        {
            Account account = accountService.findById(usdSystemAccount.getId());
            Assertions.assertEquals(1, account.getAllowNegative());
            Assertions.assertEquals(Money.of("0.00", USD), account.getBalance());
            Assertions.assertEquals("test-usa-3", account.getName());
        }
    }

    @Test
    @Order(2)
    @TransactionExplicit
    @Commit
    public void givenInitialSEKAccountBalances_whenIssuingBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        Account accountA = accountService.findById(sekAccount1.getId());
        Account accountB = accountService.findById(sekAccount2.getId());
        Account accountC = accountService.findById(sekSystemAccount.getId());

        Assertions.assertNotEquals(accountA, accountB);
        Assertions.assertNotEquals(accountA, accountC);
        Assertions.assertNotEquals(accountB, accountC);

        TransferRequest request = TransferRequest.builder()
                .withTransferType(TransferType.BANK)
                .withId(UUID.randomUUID())
                .withCity(accountC.getCity())
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity(accountA.getCity())
                .withAmount(Money.of("500.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity(accountB.getCity())
                .withAmount(Money.of("250.00", accountB.getBalance().getCurrency()))
                .withNote("credit B")
                .then()
                .addItem()
                .withId(accountC.getId())
                .withCity(accountC.getCity())
                .withAmount(Money.of("-750.00", accountC.getBalance().getCurrency()))
                .withNote("debit A+B")
                .then()
                .build();

        Transfer transfer = transferService.createTransfer(request);
        Assertions.assertNotNull(transfer);
        Assertions.assertEquals(3, transfer.getItems().size());
        Assertions.assertEquals(TransferType.BANK, transfer.getTransferType());
        Assertions.assertTrue(transfer.isNew());
    }

    @Test
    @Order(3)
    @TransactionExplicit
    @Commit
    public void givenInitialUSDAccountBalances_whenIssuingBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        Account accountA = accountService.findById(usdAccount1.getId());
        Account accountB = accountService.findById(usdAccount2.getId());
        Account accountC = accountService.findById(usdSystemAccount.getId());

        Assertions.assertNotEquals(accountA, accountB);
        Assertions.assertNotEquals(accountA, accountC);
        Assertions.assertNotEquals(accountB, accountC);

        TransferRequest request = TransferRequest.builder()
                .withTransferType(TransferType.BANK)
                .withId(UUID.randomUUID())
                .withCity(accountC.getCity())
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity(accountA.getCity())
                .withAmount(Money.of("500.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity(accountB.getCity())
                .withAmount(Money.of("250.00", accountB.getBalance().getCurrency()))
                .withNote("credit B")
                .then()
                .addItem()
                .withId(accountC.getId())
                .withCity(accountC.getCity())
                .withAmount(Money.of("-750.00", accountC.getBalance().getCurrency()))
                .withNote("debit A+B")
                .then()
                .build();

        Transfer transfer = transferService.createTransfer(request);
        Assertions.assertNotNull(transfer);
        Assertions.assertEquals(3, transfer.getItems().size());
        Assertions.assertEquals(TransferType.BANK, transfer.getTransferType());
        Assertions.assertTrue(transfer.isNew());
    }

    @Test
    @Order(4)
    @TransactionExplicit
    @Commit
    public void givenCurrentAccountBalances_whenIssuingOneBalancedFundsTransfer_thenBalancesUpdatedAndTransferRecordCreated() {
        Account accountA = accountService.findById(sekAccount1.getId());
        Account accountB = accountService.findById(sekAccount2.getId());

        Assertions.assertNotEquals(accountA, accountB);

        TransferRequest request = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity("stockholm")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity(accountA.getCity())
                .withAmount(Money.of("-50.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity(accountB.getCity())
                .withAmount(Money.of("50.00", accountB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        Transfer transfer = transferService.createTransfer(request);
        Assertions.assertNotNull(transfer);
        Assertions.assertEquals(2, transfer.getItems().size());
        Assertions.assertEquals(TransferType.PAYMENT, transfer.getTransferType());
        Assertions.assertTrue(transfer.isNew());
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
        Account sekFrom = accountService.findById(sekAccount1.getId());
        Account sekTo = accountService.findById(sekAccount2.getId());
        Account usdFrom = accountService.findById(usdAccount1.getId());
        Account usdTo = accountService.findById(usdAccount2.getId());

        Assertions.assertNotEquals(sekFrom, sekTo);
        Assertions.assertNotEquals(usdFrom, usdTo);
        Assertions.assertNotEquals(usdFrom, sekTo);
        Assertions.assertNotEquals(usdFrom, sekFrom);

        assertZeroOrPositive(sekFrom.getBalance(), "-150.00");
        assertZeroOrPositive(usdFrom.getBalance(), "-251.00");

        TransferRequest request = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withCity("stockholm")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .withTransferType(TransferType.PAYMENT)
                .addItem()
                .withId(sekFrom.getId())
                .withCity(sekFrom.getCity())
                .withAmount(Money.of("-50.00", sekFrom.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(sekFrom.getId())
                .withCity(sekFrom.getCity())
                .withAmount(Money.of("-100.00", sekFrom.getBalance().getCurrency()))
                .withNote("debit B")
                .then()
                .addItem()
                .withId(sekTo.getId())
                .withCity(sekTo.getCity())
                .withAmount(Money.of("150.00", sekTo.getBalance().getCurrency()))
                .withNote("credit A+B")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withCity(usdFrom.getCity())
                .withAmount(Money.of("-250.05", usdFrom.getBalance().getCurrency()))
                .withNote("debit C")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withCity(usdFrom.getCity())
                .withAmount(Money.of("-0.55", usdFrom.getBalance().getCurrency()))
                .withNote("debit D.1")
                .then()
                .addItem()
                .withId(usdFrom.getId())
                .withCity(usdFrom.getCity())
                .withAmount(Money.of("0.55", usdFrom.getBalance().getCurrency()))
                .withNote("debit D.2 - revert D.1")
                .then()
                .addItem()
                .withId(usdTo.getId())
                .withCity(usdTo.getCity())
                .withAmount(Money.of("250.05", usdTo.getBalance().getCurrency()))
                .withNote("credit C+D")
                .then()
                .build();

        Transfer transfer = transferService.createTransfer(request);
        Assertions.assertNotNull(transfer);
        Assertions.assertEquals(4, transfer.getItems().size());
        Assertions.assertEquals(TransferType.PAYMENT, transfer.getTransferType());
        Assertions.assertTrue(transfer.isNew());
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(6)
    public void givenCurrentAccountBalances_whenIssuingUnbalancedFundsTransfer_thenFail() {
        Account accountA = accountService.findById(sekAccount1.getId());
        Account accountB = accountService.findById(sekAccount2.getId());

        Assertions.assertNotEquals(accountA, accountB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity("stockholm")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity("stockholm")
                .withAmount(Money.of("-50.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity("stockholm")
                .withAmount(Money.of("50.05", accountB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(BadRequestException.class, () -> {
            transferService.createTransfer(form);
        });
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(7)
    public void givenCurrentAccountBalances_whenIssuingUnbalancedMultiCurrencyFundsTransfer_thenFail() {
        Account accountA = accountService.findById(sekAccount1.getId());
        Account accountB = accountService.findById(usdAccount1.getId());

        Assertions.assertNotEquals(accountA, accountB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity("stockholm")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity("stockholm")
                .withAmount(Money.of("-50.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity("stockholm")
                .withAmount(Money.of("50.00", "USD"))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(BadRequestException.class, () -> {
            transferService.createTransfer(form);
        });
    }

    @Test
    @TransactionExplicit
    @Rollback
    @Order(8)
    public void givenCurrentAccountBalances_whenIssuingFundsTransferWithNegativeBalance_thenFail() {
        Account accountA = accountService.findById(sekAccount1.getId());
        Account accountB = accountService.findById(sekAccount2.getId());

        Assertions.assertNotEquals(accountA, accountB);

        TransferRequest form = TransferRequest.builder()
                .withId(UUID.randomUUID())
                .withTransferType(TransferType.PAYMENT)
                .withCity("stockholm")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .addItem()
                .withId(accountA.getId())
                .withCity("stockholm")
                .withAmount(Money.of("-5000.00", accountA.getBalance().getCurrency()))
                .withNote("debit A")
                .then()
                .addItem()
                .withId(accountB.getId())
                .withCity("stockholm")
                .withAmount(Money.of("5000.00", accountB.getBalance().getCurrency()))
                .withNote("credit A")
                .then()
                .build();

        Assertions.assertThrows(NegativeBalanceException.class, () -> {
            transferService.createTransfer(form);
        });
    }

}
