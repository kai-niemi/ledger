package io.cockroachdb.ledger.repository.jdbc;

import java.math.BigDecimal;
import java.util.Currency;

import javax.sql.DataSource;

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

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.domain.AccountSummary;
import io.cockroachdb.ledger.domain.TransferSummary;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.repository.ReportingRepository;
import io.cockroachdb.ledger.util.MetadataUtils;
import io.cockroachdb.ledger.util.Money;

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
    public AccountSummary accountSummary(City city) {
        assertNoTransactionContext();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("city", city.getName());

        try {
            return namedParameterJdbcTemplate.queryForObject(
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
                        summary.setCity(city.getName());
                        summary.setNumberOfAccounts(rs.getInt(1));
                        summary.setTotalBalance(Money.of(rs.getBigDecimal(2), currency));
                        summary.setMinBalance(Money.of(rs.getBigDecimal(3), currency));
                        summary.setMaxBalance(Money.of(rs.getBigDecimal(4), currency));
                        summary.setUpdatedAt(rs.getTimestamp(5).toLocalDateTime());
                        return summary;
                    });
        } catch (EmptyResultDataAccessException e) {
            return AccountSummary.empty(city);
        }
    }

    @Override
    public TransferSummary transferSummary(City city) {
        assertNoTransactionContext();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("city", city.getName());

        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT "
                    + "  ti.city as city, "
                    + "  count(distinct t.id) as transfer_total, "
                    + "  count(ti.transfer_id) as total_legs, "
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
                        int colNum = 1;

                        String cityActual = rs.getString(colNum++);
                        int totalTransfers = rs.getInt(colNum++);
                        int totalLegs = rs.getInt(colNum++);
                        BigDecimal totalTurnover = rs.getBigDecimal(colNum++);
                        BigDecimal totalAmount = rs.getBigDecimal(colNum++);
                        Currency currency = Currency.getInstance(rs.getString(colNum));

                        Money turnover = Money.of(totalTurnover != null ? totalTurnover : BigDecimal.ZERO, currency);
                        // If total amount is not zero, we have a major problem (can only happen in RC w/o locks)!
                        Money checksum = Money.of(totalAmount != null ? totalAmount : BigDecimal.ZERO, currency);

                        TransferSummary summary = new TransferSummary();
                        summary.setCity(cityActual);
                        summary.setNumberOfTransfers(totalTransfers);
                        summary.setNumberOfLegs(totalLegs);
                        summary.setTotalTurnover(turnover);
                        summary.setTotalCheckSum(checksum);

                        return summary;
                    });
        } catch (EmptyResultDataAccessException e) {
            return TransferSummary.empty(city);
        }
    }
}
