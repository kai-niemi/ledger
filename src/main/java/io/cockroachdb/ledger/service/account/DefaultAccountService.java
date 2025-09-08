package io.cockroachdb.ledger.service.account;

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

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.annotations.ControlService;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.repository.AccountRepository;
import io.cockroachdb.ledger.service.NoSuchAccountException;
import io.cockroachdb.ledger.util.Money;

@Service
@ControlService
public class DefaultAccountService implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Environment environment;

    @Override
    public AccountEntity createAccount(AccountEntity accountEntity) {
        return accountRepository.createAccount(accountEntity);
    }

    @Override
    public List<UUID> createAccountBatch(Supplier<AccountEntity> factory, int batchSize) {
        return accountRepository.createAccounts(factory, batchSize);
    }

    @Override
    public AccountEntity findById(UUID id) {
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
    public AccountEntity openAccount(UUID id) {
        accountRepository.openAccount(id);
        return accountRepository.getAccountById(id)
                .orElseThrow(() -> new NoSuchAccountException(id));
    }

    @Override
    public AccountEntity closeAccount(UUID id) {
        accountRepository.closeAccount(id);
        return accountRepository.getAccountById(id)
                .orElseThrow(() -> new NoSuchAccountException(id));
    }

    @Override
    public void deleteAll() {
        accountRepository.deleteAll();
    }

    @Override
    public List<AccountEntity> findByCriteria(Set<String> cities,
                                              AccountType accountType,
                                              Pair<BigDecimal, BigDecimal> range,
                                              int limit) {
        return accountRepository.findByCriteria(cities, accountType, range, limit);
    }

    @Override
    public Page<AccountEntity> findAll(AccountType accountType, Pageable page) {
        return accountRepository.findAll(accountType, page);
    }
}
