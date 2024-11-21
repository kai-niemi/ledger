package se.cockroachdb.ledger.repository.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.model.AccountSummary;
import se.cockroachdb.ledger.model.TransferSummary;
import se.cockroachdb.ledger.repository.ReportingRepository;
import se.cockroachdb.ledger.util.MetadataUtils;
import se.cockroachdb.ledger.util.Money;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
@Profile(ProfileNames.JDBC)
public class JdbcReportingRepository implements ReportingRepository {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private boolean usingCockroachDB;

    private void assertNoTransactionContext() {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(), "TX active");
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.usingCockroachDB = MetadataUtils.isCockroachDB(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
//    @Cacheable(value = CacheConfig.CACHE_ACCOUNT_REPORT_SUMMARY)
    public Optional<AccountSummary> accountSummary(String city) {
        assertNoTransactionContext();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("city", city);

        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    "SELECT "
                            + "  count(a.id) as tot_accounts, "
                            + "  sum(a.balance) as tot_balance, "
                            + "  min(a.balance) as min_balance, "
                            + "  max(a.balance) as max_balance, "
                            + "  max(a.updated_at) as last_update, "
                            + "  a.currency as currency "
                            + "FROM account a "
                            + (usingCockroachDB ? "AS OF SYSTEM TIME follower_read_timestamp()" : "")
                            + "WHERE a.city = :city "
                            + "GROUP BY a.currency "
                            + "LIMIT 1", // Assuming single currency
                    parameters,
                    (rs, rowNum) -> {
                        Currency currency = Currency.getInstance(rs.getString(6));

                        AccountSummary summary = new AccountSummary();
                        summary.setCity(city);
                        summary.setNumberOfAccounts(rs.getInt(1));
                        summary.setTotalBalance(Money.of(rs.getBigDecimal(2), currency));
                        summary.setMinBalance(Money.of(rs.getBigDecimal(3), currency));
                        summary.setMaxBalance(Money.of(rs.getBigDecimal(4), currency));
                        summary.setUpdatedAt(rs.getTimestamp(5).toLocalDateTime());
                        return summary;
                    }));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
//    @Cacheable(value = CacheConfig.CACHE_TRANSACTION_REPORT_SUMMARY)
    public Optional<TransferSummary> transactionSummary(String city) {
        assertNoTransactionContext();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("city", city);

        // Break down per currency and use parallel queries
        try {
            return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                    "SELECT "
                            + "  count(distinct t.id) as id, "
                            + "  count(ti.transfer_id) as transfer_count, "
                            + "  sum(abs(ti.amount)) as total_turnover, "
                            + "  sum(ti.amount) as total_amount, "
                            + "  ti.currency as currency "
                            + "FROM transfer t "
                            + "  JOIN transfer_item ti ON t.id=ti.transfer_id "
                            + (usingCockroachDB ? "AS OF SYSTEM TIME follower_read_timestamp()" : "")
                            + "WHERE ti.city = :city "
                            + "GROUP BY ti.city, ti.currency "
                            + "LIMIT 1", // Assuming single currency
                    parameters,
                    (rs, rowNum) -> {
                        BigDecimal sum = rs.getBigDecimal(3);
                        BigDecimal checksum = rs.getBigDecimal(4);
                        Currency currency = Currency.getInstance(rs.getString(5));

                        TransferSummary summary = new TransferSummary();
                        summary.setCity(city);
                        summary.setNumberOfTransfers(rs.getInt(1));
                        summary.setNumberOfLegs(rs.getInt(2));
                        summary.setTotalTurnover(Money.of(sum != null ? sum : BigDecimal.ZERO, currency));
                        summary.setTotalCheckSum(Money.of(checksum != null ? checksum : BigDecimal.ZERO, currency));

                        return summary;
                    }));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
