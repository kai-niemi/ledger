package se.cockroachdb.ledger.repository.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import se.cockroachdb.ledger.annotations.EventAggregate;
import se.cockroachdb.ledger.repository.OutboxRepository;

@Repository
@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class JdbcOutboxRepository implements OutboxRepository {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void deleteAllInBatch() {
        jdbcTemplate.execute("delete from outbox where 1=1");
    }

    @Override
    public <ID> void writeEvent(EventAggregate<ID> event) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expected existing transaction - check advisor @Order");

        try {
            String payload = objectMapper.writer().writeValueAsString(event);
            jdbcTemplate.update(
                    "UPSERT INTO outbox (aggregate_type,payload) VALUES (?,?)",
                    ps -> {
                        ps.setString(1, event.getClass().getSimpleName());
                        ps.setObject(2, payload);
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing outbox JSON payload", e);
        }
    }
}
