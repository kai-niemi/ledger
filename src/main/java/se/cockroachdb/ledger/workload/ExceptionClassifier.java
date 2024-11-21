package se.cockroachdb.ledger.workload;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.TransientDataAccessException;

public interface ExceptionClassifier {
    /**
     * Only 40001 is safe to retry in terms of non-idempotent side effects (like INSERT:s)
     */
    List<String> TRANSIENT_CODES = List.of(
            "40001", "08001", "08003", "08004", "08006", "08007", "08S01", "57P01"
    );

    default boolean isTransient(Throwable ex) {
        if (ex instanceof TransientDataAccessException) {
            return true;
        }

        if (ex instanceof SQLException) {
            String sqlState = ((SQLException) ex).getSQLState();
            if (TRANSIENT_CODES.contains(sqlState)) {
                return true;
            }
        }

        return false;
    }
}
