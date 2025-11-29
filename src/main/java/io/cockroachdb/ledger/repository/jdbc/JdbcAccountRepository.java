package io.cockroachdb.ledger.repository.jdbc;

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

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.repository.AccountRepository;
import io.cockroachdb.ledger.util.Money;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
@Profile(ProfileNames.NOT_JPA)
public class JdbcAccountRepository implements AccountRepository {
    private JdbcTemplate jdbcTemplate;

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public AccountEntity createAccount(AccountEntity accountEntity) {
        jdbcTemplate.update("INSERT INTO account "
                            + "(id, city, balance, currency, name, description, type, closed, allow_negative) "
                            + "VALUES(?,?,?,?,?,?,?::account_type,?,?)",
                accountEntity.getId(),
                accountEntity.getCity(),
                accountEntity.getBalance().getAmount(),
                accountEntity.getBalance().getCurrency().getCurrencyCode(),
                accountEntity.getName(),
                accountEntity.getDescription(),
                accountEntity.getAccountType().getCode(),
                accountEntity.isClosed(),
                accountEntity.getAllowNegative()
        );
        return accountEntity;
    }

    @Override
    public List<UUID> createAccounts(Supplier<AccountEntity> factory, int batchSize) {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(), "Expected no transaction");
        List<UUID> ids = new ArrayList<>();
        jdbcTemplate.batchUpdate(
                "INSERT INTO account "
                + "(id, city, balance, currency, name, description, type, closed, allow_negative) "
                + "VALUES(?,?,?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AccountEntity accountEntity = factory.get();
                        int idx = 1;

                        ps.setObject(idx++, accountEntity.getId());
                        ps.setString(idx++, accountEntity.getCity());
                        ps.setBigDecimal(idx++, accountEntity.getBalance().getAmount());
                        ps.setString(idx++, accountEntity.getBalance().getCurrency().getCurrencyCode());
                        ps.setString(idx++, accountEntity.getName());
                        ps.setString(idx++, accountEntity.getDescription());
                        ps.setString(idx++, accountEntity.getAccountType().getCode());
                        ps.setBoolean(idx++, accountEntity.isClosed());
                        ps.setInt(idx, accountEntity.getAllowNegative());

                        ids.add(accountEntity.getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return batchSize;
                    }
                });
        return ids;
    }

    @Override
    public void updateBalances(Map<UUID, BigDecimal> balanceUpdates) {
        int rows = jdbcTemplate.update(
                "UPDATE account SET balance = account.balance + data_table.balance, updated_at=clock_timestamp() "
                + "FROM "
                + "(select unnest(?) as id, unnest(?) as balance) as data_table "
                + "WHERE account.id=data_table.id "
                + "AND (account.balance + data_table.balance) * abs(account.allow_negative-1) >= 0",
                ps -> {
                    List<UUID> ids = new ArrayList<>();
                    List<BigDecimal> balances = new ArrayList<>();

                    balanceUpdates
                            .forEach((uuid, amount) -> {
                                ids.add(uuid);
                                balances.add(amount);
                            });

                    ps.setArray(1, ps.getConnection()
                            .createArrayOf("UUID", ids.toArray()));
                    ps.setArray(2, ps.getConnection()
                            .createArrayOf("DECIMAL", balances.toArray()));
                });

        if (rows != balanceUpdates.size()) {
            throw new IncorrectResultSizeDataAccessException(balanceUpdates.size(), rows);
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
    public Optional<AccountEntity> getAccountById(UUID id) {
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

    private AccountEntity readAccount(ResultSet rs) throws SQLException {
        Money balance = Money.of(
                rs.getBigDecimal("balance"),
                rs.getString("currency"));

        return AccountEntity.builder()
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
    public List<AccountEntity> findByCriteria(Set<String> cities,
                                              AccountType accountType,
                                              Pair<BigDecimal, BigDecimal> range,
                                              int limit) {
        if (cities.isEmpty()) {
            return List.of();
        }

        String sql;

        // Use CTE window function to sort and limit by city to avoid full scans.
        // Equality cancels out balance range filtering.
        if (range.getFirst().equals(range.getSecond())) {
            sql = "WITH accounts AS ( " +
                  "SELECT *, ROW_NUMBER() OVER (PARTITION BY city ORDER BY id) n " +
                  "FROM account WHERE city IN (:cities) "
                  + "AND account.type = :type) " +
                  "SELECT * " +
                  "FROM accounts " +
                  "WHERE n <= :limit " +
                  "ORDER BY city";
        } else {
            sql = "WITH accounts AS ( " +
                  "SELECT *, ROW_NUMBER() OVER (PARTITION BY city ORDER BY id) n " +
                  "FROM account WHERE city IN (:cities) "
                  + "AND account.balance BETWEEN :min AND :max "
                  + "AND account.type = :type) " +
                  "SELECT * " +
                  "FROM accounts " +
                  "WHERE n <= :limit " +
                  "ORDER BY city";
        }

        return this.namedParameterJdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("cities", cities)
                        .addValue("min", range.getFirst())
                        .addValue("max", range.getSecond())
                        .addValue("type", accountType.getCode())
                        .addValue("limit", limit),
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public List<AccountEntity> findById(Set<UUID> ids, boolean forUpdate) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Expected transaction");

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);

        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE id in (:ids) "
                + (forUpdate ? " FOR UPDATE" : ""),
                parameters,
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public Page<AccountEntity> findAll(AccountType accountType, Pageable page) {
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

        List<AccountEntity> accountEntityEntities = this.namedParameterJdbcTemplate
                .query(sql, parameters, (rs, rowNum) -> readAccount(rs));

        Long total = this.namedParameterJdbcTemplate.queryForObject(
                "SELECT count(id) FROM account a "
                + "WHERE a.type = :type",
                parameters,
                Long.class);

        return new PageImpl<>(accountEntityEntities, page, total);
    }
}
