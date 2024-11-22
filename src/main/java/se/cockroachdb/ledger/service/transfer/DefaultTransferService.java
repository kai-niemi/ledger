package se.cockroachdb.ledger.service.transfer;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import se.cockroachdb.ledger.ApplicationProperties;
import se.cockroachdb.ledger.annotations.ControlService;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.model.AccountItem;
import se.cockroachdb.ledger.model.TransferRequest;
import se.cockroachdb.ledger.repository.AccountRepository;
import se.cockroachdb.ledger.repository.TransferRepository;
import se.cockroachdb.ledger.service.BadRequestException;
import se.cockroachdb.ledger.service.NegativeBalanceException;
import se.cockroachdb.ledger.shell.support.JsonHelper;
import se.cockroachdb.ledger.util.Money;

@ControlService
public class DefaultTransferService implements TransferService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private ApplicationProperties applicationModel;

    @Override
    @Transactional(propagation = Propagation.MANDATORY) // to signal txn required
    public Transfer createTransfer(TransferRequest transferRequest) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected transaction context");

        // Validate request first
        preValidateInvariants(transferRequest.getAccountItems());

        // Short-circuit if seen before with a shallow, transient copy.
        // For formally correct idempotency it's required to return the existing transfer with legs.
        boolean exists = applicationModel.isIdempotencyCheck() && idempotencyCheck(transferRequest.getId(),
                transferRequest.getCity());
        if (exists) {
            return Transfer.builder()
                    .withId(transferRequest.getId())
                    .withCity(transferRequest.getCity())
                    .build();
        }

        final Set<UUID> accountIds = transferRequest.getAccountItems()
                .stream()
                .map(AccountItem::getId)
                .collect(Collectors.toSet());

        final Set<String> accountCities = transferRequest.getAccountItems()
                .stream()
                .map(AccountItem::getCity)
                .collect(Collectors.toSet());

        final Map<UUID, AccountItem> itemsPerAccountId = transferRequest.getAccountItems()
                .stream()
                .collect(Collectors.toMap(AccountItem::getId,
                        accountItem -> accountItem, (x, b) -> b));

        // Business validation complete, let's go ahead with DB reads/writes and defer the rest to DB constraints

        final List<Account> accounts
                = accountRepository.findById(accountCities, accountIds, applicationModel.isUsingLocks());

        if (accounts.size() != accountIds.size()) {
            throw new BadRequestException("Expected %d accounts, found %d"
                    .formatted(accountIds.size(), accounts.size()));
        }

        final Transfer.Builder transferBuilder = Transfer.builder()
                .withCity(transferRequest.getCity())
                .withTransferType(transferRequest.getTransferType())
                .withBookingDate(transferRequest.getBookingDate())
                .withTransferDate(transferRequest.getTransferDate());

        // First create transfer record so we can use its ID to stitch the transfer legs
        final Transfer transfer
                = transferRepository.createTransfer(transferBuilder.build());

        // Then create transfer items or legs, describing the monetary transfer and
        // storing a running balance (balance before update).
        final TransferItem.Builder itemBuilder = TransferItem.builder().withTransfer(transfer);
        {
            accounts.forEach(account -> {
                AccountItem accountItem = itemsPerAccountId.get(account.getId());
                itemBuilder
                        .withTransfer(transfer)
                        .withCity(transferRequest.getCity())
                        .withAccount(account)
                        .withRunningBalance(account.getBalance())
                        .withAmount(accountItem.getAmount())
                        .withNote(accountItem.getNote())
                        .and();
            });
        }

        // Write the transfer items
        List<TransferItem> items = transferRepository.createTransferItems(itemBuilder.build());
        transfer.addItems(items);

        // Update the account balances in one batch
        try {
            accountRepository.updateBalances(coalesceItems(transferRequest.getAccountItems()));
        } catch (IncorrectResultSizeDataAccessException e) {
            logger.warn("Negative balance outcome for denied transfer request:\n%s".formatted(
                    JsonHelper.toFormattedJSON(transferRequest)
            ));
            throw new NegativeBalanceException("Negative balance check constraint failed - check log", e);
        }

        return transfer;
    }

    private void preValidateInvariants(List<AccountItem> accountItems) {
        if (accountItems.size() < 2) {
            throw new BadRequestException("Must have at least two account legs but got: "
                                          + accountItems.size());
        }

        final Map<Currency, BigDecimal> checksumPerCurrency = new HashMap<>();

        accountItems.forEach(accountItem -> {
            Assert.notNull(accountItem.getId(), "account leg id is null");
            Assert.notNull(accountItem.getCity(), "account leg city is null");

            // Zero-balance invariant
            checksumPerCurrency.compute(accountItem.getAmount().getCurrency(),
                    (currency, checksum) ->
                            (checksum == null)
                                    ? accountItem.getAmount().getAmount()
                                    : accountItem.getAmount().getAmount().add(checksum));
        });

        // The sum of debits for all accounts must equal the corresponding sum of credits (per currency)
        checksumPerCurrency.forEach((key, value) -> {
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException(
                        "Unbalanced transaction: currency [" + key + "], amount sum [" + value + "]");
            }
        });
    }

    /**
     * Simple point lookup for idempotency.
     *
     * @param transferId client specified transfer ID
     * @param city       transfer record home
     * @return optional transfer
     */
    private boolean idempotencyCheck(UUID transferId, String city) {
        return transferRepository.checkTransferExists(transferId, city);
    }

    /**
     * Coalesce transfer account items / legs by grouping account IDs with leg sum's.
     *
     * @param accountItems the items
     * @return map of account IDs to update tuples (city and sum)
     */
    private Map<UUID, Pair<String, BigDecimal>> coalesceItems(List<AccountItem> accountItems) {
        final Map<UUID, AccountItem> itemsPerAccountId = accountItems
                .stream()
                .collect(Collectors.toMap(AccountItem::getId,
                        accountItem -> accountItem, (x, b) -> b));

        final Map<UUID, Pair<String, BigDecimal>> balanceUpdates = new HashMap<>();
        Map<UUID, List<AccountItem>> accountsPerId = accountItems
                .stream()
                .collect(Collectors.groupingBy(AccountItem::getId));

        accountsPerId.forEach((uuid, items) -> {
            BigDecimal sum = items.stream()
                    .map(AccountItem::getAmount)
                    .map(Money::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String city = itemsPerAccountId.get(uuid).getCity();
            balanceUpdates.put(uuid, Pair.of(city, sum));
        });

        return balanceUpdates;
    }

    @Override
    public Page<Transfer> findAll(TransferType transferType, Pageable page) {
        return transferRepository.findAllTransfers(transferType, page);
    }

    @Override
    public Page<Transfer> findAllByAccountId(UUID accountId, Pageable page) {
        return transferRepository.findAllTransfersByAccountId(accountId, page);
    }

    @Override
    public Page<Transfer> findAllByCity(String city, Pageable page) {
        return transferRepository.findAllTransfersByCity(city, page);
    }

    @Override
    public Transfer findById(UUID id) {
        return transferRepository.findTransferById(id);
    }

    @Override
    public Page<TransferItem> findAllItems(UUID transferId, Pageable page) {
        return transferRepository.findAllTransferItems(transferId, page);
    }

    @Override
    public void deleteAll() {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Expected transaction");
        transferRepository.deleteAll();
    }
}
