package se.cockroachdb.ledger.repository.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.repository.AccountRepository;
import se.cockroachdb.ledger.util.Money;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
@Profile(ProfileNames.JDBC)
public class JdbcAccountRepository implements AccountRepository {
    private JdbcTemplate jdbcTemplate;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Account createAccount(Account account) {
        jdbcTemplate.update("INSERT INTO account "
                            + "(id, city, balance, currency, name, description, type, closed, allow_negative) "
                            + "VALUES(?,?,?,?,?,?,?::account_type,?,?)",
                account.getId(),
                account.getCity(),
                account.getBalance().getAmount(),
                account.getBalance().getCurrency().getCurrencyCode(),
                account.getName(),
                account.getDescription(),
                account.getAccountType().getCode(),
                account.isClosed(),
                account.getAllowNegative()
        );
        return account;
    }

    @Override
    public List<UUID> createAccounts(Supplier<Account> factory, int batchSize) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(), "Expected no transaction");
        List<UUID> ids = new ArrayList<>();
        jdbcTemplate.batchUpdate(
                "INSERT INTO account "
                + "(id,city, balance, currency, name, description, type, closed, allow_negative) "
                + "VALUES(?,?,?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Account account = factory.get();
                        int idx = 1;
                        ps.setObject(idx++, account.getId());
                        ps.setString(idx++, account.getCity());
                        ps.setBigDecimal(idx++, account.getBalance().getAmount());
                        ps.setString(idx++, account.getBalance().getCurrency().getCurrencyCode());
                        ps.setString(idx++, account.getName());
                        ps.setString(idx++, account.getDescription());
                        ps.setString(idx++, account.getAccountType().getCode());
                        ps.setBoolean(idx++, account.isClosed());
                        ps.setInt(idx, account.getAllowNegative());
                        ids.add(account.getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return batchSize;
                    }
                });
        return ids;
    }

    @Override
    public void updateBalances(Map<UUID, Pair<String, BigDecimal>> accountUpdates) {
        int rows = jdbcTemplate.update(
                "UPDATE account SET balance = account.balance + data_table.balance, updated_at=clock_timestamp() "
                + "FROM "
                + "(select unnest(?) as id, unnest(?) as balance, unnest(?) as city) as data_table "
                + "WHERE account.id=data_table.id "
                + "AND account.city=data_table.city "
                + "AND (account.balance + data_table.balance) * abs(account.allow_negative-1) >= 0",
                ps -> {
                    List<UUID> ids = new ArrayList<>();
                    List<BigDecimal> balances = new ArrayList<>();
                    List<String> cities = new ArrayList<>();

                    accountUpdates
                            .forEach((uuid, pair) -> {
                                ids.add(uuid);
                                cities.add(pair.getFirst());
                                balances.add(pair.getSecond());
                            });

                    ps.setArray(1, ps.getConnection()
                            .createArrayOf("UUID", ids.toArray()));
                    ps.setArray(2, ps.getConnection()
                            .createArrayOf("DECIMAL", balances.toArray()));
                    ps.setArray(3, ps.getConnection()
                            .createArrayOf("VARCHAR", cities.toArray()));
                });

        if (rows != accountUpdates.size()) {
            throw new IncorrectResultSizeDataAccessException(accountUpdates.size(), rows);
        }
    }

    @Override
    public void closeAccount(UUID id) {
        int rowsAffected = jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE account "
                            + "SET closed=true "
                            + "WHERE id=?");
                    ps.setObject(1, id);
                    return ps;
                });
        if (rowsAffected != 1) {
            throw new IncorrectResultSizeDataAccessException(1, rowsAffected);
        }
    }

    @Override
    public void openAccount(UUID id) {
        int rowsAffected = jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "UPDATE account "
                            + "SET closed=false "
                            + "WHERE id=?");
                    ps.setObject(1, id);
                    return ps;
                });
        if (rowsAffected != 1) {
            throw new IncorrectResultSizeDataAccessException(1, rowsAffected);
        }
    }

    @Override
    public Optional<Account> getAccountById(UUID id) {
        try {
            return Optional.ofNullable(this.jdbcTemplate.queryForObject(
                    "SELECT * FROM account WHERE id=?",
                    (rs, rowNum) -> readAccount(rs),
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Money getBalance(UUID id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT balance,currency FROM account WHERE id=?",
                (rs, rowNum) -> Money.of(rs.getBigDecimal(1), rs.getString(2)),
                id
        );
    }

    @Override
    public Money getBalanceSnapshot(UUID id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT balance,currency "
                + "FROM account "
                + "AS OF SYSTEM TIME follower_read_timestamp() "
                + "WHERE id=?",
                (rs, rowNum) -> Money.of(rs.getBigDecimal(1), rs.getString(2)),
                id
        );
    }

    private Account readAccount(ResultSet rs) throws SQLException {
        Money balance = Money.of(
                rs.getBigDecimal("balance"),
                rs.getString("currency"));

        return Account.builder()
                .withId((UUID) rs.getObject("id"))
                .withCity(rs.getString("city"))
                .withName(rs.getString("name"))
                .withBalance(balance)
                .withAccountType(AccountType.of(rs.getString("type")))
                .withDescription(rs.getString("description"))
                .withClosed(rs.getBoolean("closed"))
                .withAllowNegative(rs.getInt("allow_negative") > 0)
                .withUpdated(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE account");
    }

    @Override
    public List<Account> findByCriteria(Set<String> cities, AccountType accountType, int limit) {
        if (cities.isEmpty()) {
            return List.of();
        }
        // Use CTE window function to sort and limit by city
        return this.namedParameterJdbcTemplate.query(
                "WITH accounts AS ( " +
                "SELECT *, ROW_NUMBER() OVER (PARTITION BY city ORDER BY id) n " +
                "FROM account WHERE city IN (:cities) AND account.type = :type) " +
                "SELECT * " +
                "FROM accounts " +
                "WHERE n <= :limit " +
                "ORDER BY city",
                new MapSqlParameterSource()
                        .addValue("cities", cities)
                        .addValue("type", accountType.getCode())
                        .addValue("limit", limit),
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public List<Account> findByCriteria(String city, AccountType accountType,
                                        Pair<BigDecimal, BigDecimal> range,
                                        int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("type", accountType.getCode())
                .addValue("city", city)
                .addValue("min", range.getFirst())
                .addValue("max", range.getSecond())
                .addValue("limit", limit);

        return this.namedParameterJdbcTemplate
                .query("SELECT * "
                       + "FROM account a "
                       + "WHERE a.type = :type "
                       + "and a.city = :city "
                       + "and a.balance between :min and :max "
                       + "ORDER BY id "
                       + "LIMIT :limit",
                        parameters, (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public List<Account> findById(Set<String> cities, Set<UUID> ids, boolean forUpdate) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Expected transaction");

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);
        parameters.addValue("city", cities);

        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE id in (:ids) and city in (:city) "
                + (forUpdate ? " FOR UPDATE" : ""),
                parameters,
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public Page<Account> findAll(AccountType accountType, Pageable page) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("type", accountType.getCode())
                .addValue("limit", page.getPageSize())
                .addValue("offset", page.getOffset());

        String sql =
                "SELECT * "
                + "FROM account a "
                + "WHERE a.type = :type "
                + "ORDER BY id, city "
                + "LIMIT :limit OFFSET :offset ";

        List<Account> accountEntities = this.namedParameterJdbcTemplate
                .query(sql, parameters, (rs, rowNum) -> readAccount(rs));

        Long total = this.namedParameterJdbcTemplate.queryForObject(
                "SELECT count(id) FROM account a "
                + "WHERE a.type = :type",
                parameters,
                Long.class);

        return new PageImpl<>(accountEntities, page, total);
    }

    @Override
    public Page<Account> findAll(Set<String> cities, Pageable page) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("cities", cities.stream())
                .addValue("limit", page.getPageSize())
                .addValue("offset", page.getOffset());

        String sql =
                "SELECT * "
                + "FROM account "
                + "WHERE account.city in (:cities) "
                + "ORDER BY id, city "
                + "LIMIT :limit OFFSET :offset ";

        List<Account> accountEntities = this.namedParameterJdbcTemplate
                .query(sql, parameters, (rs, rowNum) -> readAccount(rs));

        Long total = this.namedParameterJdbcTemplate.queryForObject(
                "SELECT count(id) FROM account WHERE city in (:cities)",
                parameters,
                Long.class);

        return new PageImpl<>(accountEntities, page, total);
    }
}
