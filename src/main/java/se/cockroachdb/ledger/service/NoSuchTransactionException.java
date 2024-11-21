package se.cockroachdb.ledger.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such transfer")
public class NoSuchTransactionException extends BusinessException {
    public NoSuchTransactionException(String id) {
        super("No such transaction: " + id);
    }
}
