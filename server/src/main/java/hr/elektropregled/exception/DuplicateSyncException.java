package hr.elektropregled.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSyncException extends BusinessException {
    public DuplicateSyncException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}