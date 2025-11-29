package io.cockroachdb.ledger.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferItemEntity;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.repository.TransferRepository;
import io.cockroachdb.ledger.util.Money;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
@Profile(ProfileNames.NOT_JPA)
public class JdbcTransferRepository implements TransferRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public TransferEntity createTransfer(TransferEntity transferEntity) {
        final LocalDate bookingDate = transferEntity.getBookingDate();
        final LocalDate transferDate = transferEntity.getTransferDate();

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO transfer "
                                                         + "(city,booking_date,transfer_date,transfer_type) "
                                                         + "VALUES(?,?,?,?::transfer_type) returning id::uuid",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setObject(1, transferEntity.getCity());
            ps.setObject(2, bookingDate != null ? bookingDate : LocalDate.now());
            ps.setObject(3, transferDate != null ? transferDate : LocalDate.now());
            ps.setObject(4, transferEntity.getTransferType().getCode());
            return ps;
        }, keyHolder);

        transferEntity.setId(keyHolder.getKeyAs(UUID.class));

        return transferEntity;
    }

    @Override
    public List<TransferItemEntity> createTransferItems(List<TransferItemEntity> items) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO transfer_item "
                + "(transfer_id, city, item_pos, account_id, amount, currency, note, running_balance) "
                + "VALUES(?,?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TransferItemEntity item = items.get(i);

                        ps.setObject(1, item.getTransferEntity().getId());
                        ps.setString(2, item.getCity());
                        ps.setInt(3, i);
                        ps.setObject(4, item.getAccountEntity().getId());
                        ps.setBigDecimal(5, item.getAmount().getAmount());
                        ps.setString(6, item.getAmount().getCurrency().getCurrencyCode());
                        ps.setString(7, item.getNote());
                        ps.setBigDecimal(8, item.getRunningBalance().getAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });
        return items;
    }

    @Override
    public TransferEntity findTransferById(UUID transferId) {
        return DataAccessUtils.singleResult(this.jdbcTemplate.query(
                "SELECT * FROM transfer WHERE id=?",
                (rs, rowNum) -> mapTransfer(rs, true),
                transferId));
    }

    @Override
    public boolean checkTransferExists(UUID requestId) {
        return Boolean.TRUE.equals(this.jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM transfer WHERE id=?)",
                Boolean.class,
                requestId));
    }

    @Override
    public Page<TransferEntity> findAllTransfersByAccountId(UUID accountId, Pageable pageable) {
        List<Integer> results = this.jdbcTemplate.query(
                "SELECT count(1) FROM transfer t JOIN transfer_item ti ON t.id = ti.transfer_id "
                + "WHERE ti.account_id = ?",
                (rs, rowNum) -> rs.getInt(1),
                accountId);

        Integer count = DataAccessUtils.nullableSingleResult(results);

        List<TransferEntity> content = this.jdbcTemplate.query(
                "SELECT t.* FROM transfer t JOIN transfer_item ti ON t.id = ti.transfer_id "
                + "WHERE ti.account_id = ? "
                + "ORDER BY transfer_date LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapTransfer(rs, false),
                accountId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(content, pageable, count != null ? count : 0);
    }

    @Override
    public Page<TransferEntity> findAllTransfersByCity(String city, Pageable pageable) {
        List<Integer> results = this.jdbcTemplate.query(
                "SELECT count(1) FROM transfer t "
                + "WHERE t.city = ?",
                (rs, rowNum) -> rs.getInt(1),
                city);

        Integer count = DataAccessUtils.singleResult(results);

        List<TransferEntity> content = this.jdbcTemplate.query(
                "SELECT t.*  FROM transfer t "
                + "WHERE t.city = ? "
                + "ORDER BY transfer_date LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapTransfer(rs, false),
                city, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(content, pageable, count != null ? count : 0);
    }

    @Override
    public Page<TransferEntity> findAllTransfers(TransferType transferType, Pageable pageable) {
        int count = countAllTransfers(transferType);

        List<TransferEntity> content = this.jdbcTemplate.query(
                "SELECT * FROM transfer "
                + "WHERE transfer_type=? "
                + "ORDER BY transfer_date LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapTransfer(rs, false),
                transferType.getCode(),
                pageable.getPageSize(),
                pageable.getOffset());

        return new PageImpl<>(content, pageable, count);
    }

    private Integer countAllTransfers(TransferType transferType) {
        List<Integer> results = this.jdbcTemplate.query(
                "SELECT count(id) FROM transfer "
                + "WHERE transfer_type=?",
                (rs, rowNum) -> rs.getInt(1),
                transferType.getCode());
        return DataAccessUtils.singleResult(results);
    }

    @Override
    public Page<TransferItemEntity> findAllTransferItems(UUID transferId, Pageable pageable) {
        long count = countItemsByTransferId(transferId);

        List<TransferItemEntity> content = this.jdbcTemplate.query(
                "SELECT * FROM transfer_item WHERE transfer_id=?",
                (rs, rowNum) -> mapTransferItem(rs, rowNum),
                transferId
        );

        return new PageImpl<>(content, pageable, count);
    }

    private Long countItemsByTransferId(UUID id) {
        List<Long> results =
                this.jdbcTemplate.query(
                        "SELECT count(transfer_id) FROM transfer_item WHERE transfer_id=?",
                        (rs, rowNum) -> rs.getLong(1),
                        id
                );
        return DataAccessUtils.singleResult(results);
    }

    private TransferEntity mapTransfer(ResultSet rs, boolean includeItems) throws SQLException {
        UUID transferId = (UUID) rs.getObject("id");
        String city = rs.getString("city");
        TransferType transferType = TransferType.of(rs.getString("transfer_type"));
        LocalDate bookingDate = rs.getDate("booking_date").toLocalDate();
        LocalDate transferDate = rs.getDate("transfer_date").toLocalDate();

        // N+1

        TransferEntity t = TransferEntity.builder()
                .withId(transferId)
                .withCity(city)
                .withTransferType(transferType)
                .withBookingDate(bookingDate)
                .withTransferDate(transferDate)
                .build();

        if (includeItems) {
            t.addItems(findTransferItems(transferId));
        }

        return t;
    }

    private List<TransferItemEntity> findTransferItems(UUID id) {
        return this.jdbcTemplate.query(
                "SELECT * FROM transfer_item WHERE transfer_id=?",
                (rs, rowNum) -> mapTransferItem(rs, rowNum),
                id
        );
    }

    private TransferItemEntity mapTransferItem(ResultSet rs, int rowNum) throws SQLException {
        UUID accountId = (UUID) rs.getObject("account_id");
        UUID transferId = (UUID) rs.getObject("transfer_id");
        String city = rs.getString("city");
        Money amount = Money.of(rs.getBigDecimal("amount"), rs.getString("currency"));
        Money runningBalance = Money.of(rs.getBigDecimal("running_balance"), rs.getString("currency"));
        String note = rs.getString("note");

        // Shallow to avoid N+1
        TransferEntity transferEntity = TransferEntity.builder().withId(transferId).build();
        // Shallow to avoid N+1
        AccountEntity accountEntity = AccountEntity.builder().withId(accountId).build();

        TransferItemEntity item = new TransferItemEntity(transferEntity, accountEntity, rowNum);
        item.setCity(city);
        item.setAmount(amount);
        item.setRunningBalance(runningBalance);
        item.setNote(note);

        return item;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE transfer_item");
        jdbcTemplate.execute("TRUNCATE TABLE transfer");
    }
}
