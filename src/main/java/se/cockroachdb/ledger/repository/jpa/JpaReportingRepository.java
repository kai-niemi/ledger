package se.cockroachdb.ledger.repository.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.model.AccountSummary;
import se.cockroachdb.ledger.model.TransferSummary;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.util.Money;

@Repository
@Profile(ProfileNames.JPA)
public class JpaReportingRepository implements ReportingRepository {
    @Autowired
    private AccountJpaRepository accountRepository;

    @Override
    public Optional<AccountSummary> accountSummary(String city) {
        List<AccountSummary> result = new LinkedList<>();

        accountRepository.accountSummary(city)
                .forEach(tuple -> {
            Currency currency = tuple.get(6, Currency.class);

            AccountSummary summary = new AccountSummary();
            summary.setCity(city);
            summary.setNumberOfAccounts(tuple.get(0, Long.class));
            summary.setTotalBalance(Money.of(tuple.get(2, BigDecimal.class), currency));
            summary.setMinBalance(Money.of(tuple.get(3, BigDecimal.class), currency));
            summary.setMaxBalance(Money.of(tuple.get(4, BigDecimal.class), currency));
            summary.setUpdatedAt(tuple.get(5, LocalDateTime.class));

            result.add(summary);
        });

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(result.iterator().next());
    }

    @Override
    public Optional<TransferSummary> transactionSummary(String city) {
        List<TransferSummary> result = new LinkedList<>();

        accountRepository.transactionSummary(city).forEach(tuple -> {
            BigDecimal sum = tuple.get(2, BigDecimal.class);
            BigDecimal checksum = tuple.get(3, BigDecimal.class);
            Currency currency = tuple.get(4, Currency.class);

            TransferSummary summary = new TransferSummary();
            summary.setCity(city);
            summary.setNumberOfTransfers(tuple.get(0, Long.class));
            summary.setNumberOfLegs(tuple.get(1, Long.class));
            summary.setTotalTurnover(Money.of(sum != null ? sum : BigDecimal.ZERO, currency));
            summary.setTotalCheckSum(Money.of(checksum != null ? checksum : BigDecimal.ZERO, currency));

            result.add(summary);
        });

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(result.iterator().next());
    }
}
