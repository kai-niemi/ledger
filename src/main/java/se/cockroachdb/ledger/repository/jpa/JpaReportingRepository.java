package se.cockroachdb.ledger.repository.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.domain.AccountSummary;
import se.cockroachdb.ledger.domain.TransferSummary;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.util.Money;

@Repository
@Profile(ProfileNames.JPA)
public class JpaReportingRepository implements ReportingRepository {
    @Autowired
    private AccountJpaRepository accountRepository;

    @Override
    public AccountSummary accountSummary(City city) {
        List<AccountSummary> result = new LinkedList<>();

        accountRepository.accountSummary(city.getName())
                .forEach(tuple -> {
            Currency currency = tuple.get(6, Currency.class);

            AccountSummary summary = new AccountSummary();
            summary.setCity(city.getName());
            summary.setNumberOfAccounts(tuple.get(0, Long.class));
            summary.setTotalBalance(Money.of(tuple.get(2, BigDecimal.class), currency));
            summary.setMinBalance(Money.of(tuple.get(3, BigDecimal.class), currency));
            summary.setMaxBalance(Money.of(tuple.get(4, BigDecimal.class), currency));
            summary.setUpdatedAt(tuple.get(5, LocalDateTime.class));

            result.add(summary);
        });

        if (result.isEmpty()) {
            return  AccountSummary.empty(city);
        }

        return result.iterator().next();
    }

    @Override
    public TransferSummary transferSummary(City city) {
        List<TransferSummary> result = new LinkedList<>();

        accountRepository.transactionSummary(city.getName()).forEach(tuple -> {
            BigDecimal sum = tuple.get(2, BigDecimal.class);
            BigDecimal checksum = tuple.get(3, BigDecimal.class);
            Currency currency = tuple.get(4, Currency.class);

            TransferSummary summary = new TransferSummary();
            summary.setCity(city.getName());
            summary.setNumberOfTransfers(tuple.get(0, Long.class));
            summary.setNumberOfLegs(tuple.get(1, Long.class));
            summary.setTotalTurnover(Money.of(sum != null ? sum : BigDecimal.ZERO, currency));
            summary.setTotalCheckSum(Money.of(checksum != null ? checksum : BigDecimal.ZERO, currency));

            result.add(summary);
        });

        if (result.isEmpty()) {
            return TransferSummary.empty(city);
        }

        return result.iterator().next();
    }
}
