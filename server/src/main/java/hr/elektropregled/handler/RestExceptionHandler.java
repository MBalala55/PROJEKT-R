package hr.elektropregled.handler;

import hr.elektropregled.dto.SyncResponse;
import hr.elektropregled.exception.BusinessException;
import hr.elektropregled.exception.DuplicateSyncException;
import hr.elektropregled.exception.NotFoundException;
import hr.elektropregled.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<SyncResponse> handleBusiness(BusinessException ex) {
        return buildError(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SyncResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Neispravan zahtjev");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SyncResponse> handleOther(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Neočekivana pogreška: " + ex.getMessage());
    }

    private ResponseEntity<SyncResponse> buildError(HttpStatus status, String message) {
        SyncResponse error = new SyncResponse();
        error.setSuccess(false);
        error.setMessage(message);
        error.setTimestamp(Instant.now());
        return ResponseEntity.status(status).body(error);
    }
}