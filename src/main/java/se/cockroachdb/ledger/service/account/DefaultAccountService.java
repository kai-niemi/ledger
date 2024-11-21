package se.cockroachdb.ledger.service.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.annotations.ControlService;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.repository.AccountRepository;
import se.cockroachdb.ledger.service.NoSuchAccountException;
import se.cockroachdb.ledger.util.Money;

@Service
@ControlService
public class DefaultAccountService implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Environment environment;

    @Override
    public Account createAccount(Account account) {
        return accountRepository.createAccount(account);
    }

    @Override
    public List<UUID> createAccountBatch(Supplier<Account> factory, int batchSize) {
        return accountRepository.createAccounts(factory, batchSize);
    }

    @Override
    public Account findById(UUID id) {
        return accountRepository.getAccountById(id)
                .orElseThrow(() -> new NoSuchAccountException(id));
    }

    @Override
    public Money getBalance(UUID id) {
        return accountRepository.getBalance(id);
    }

    @Override
    public Money getBalanceSnapshot(UUID id) {
        if (ProfileNames.acceptsPostgresSQL(environment)) {
            return getBalance(id);
        }
        return accountRepository.getBalanceSnapshot(id);
    }

    @Override
    public Account openAccount(UUID id) {
        accountRepository.openAccount(id);
        return accountRepository.getAccountById(id)
                .orElseThrow(() -> new NoSuchAccountException(id));
    }

    @Override
    public Account closeAccount(UUID id) {
        accountRepository.closeAccount(id);
        return accountRepository.getAccountById(id)
                .orElseThrow(() -> new NoSuchAccountException(id));
    }

    @Override
    public void deleteAll() {
        accountRepository.deleteAll();
    }

    @Override
    public List<Account> findByCriteria(Set<String> cities, AccountType accountType, int limit) {
        return accountRepository.findByCriteria(cities, accountType, limit);
    }

    @Override
    public List<Account> findByCriteria(String city, AccountType accountType, Pair<BigDecimal, BigDecimal> range,
                                        int limit) {
        return accountRepository.findByCriteria(city, accountType, range, limit);
    }

    @Override
    public Page<Account> findAll(AccountType accountType, Pageable page) {
        return accountRepository.findAll(accountType, page);
    }

    @Override
    public Page<Account> findAll(Set<String> cities, Pageable page) {
        return accountRepository.findAll(cities, page);
    }
}
