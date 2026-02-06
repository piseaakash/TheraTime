package com.theratime.exception;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
        meterRegistry.counter("exceptions.total", "type", "ResourceNotFound").increment();

        log.warn("Resource not found: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        buildErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException exception) {
        meterRegistry.counter("exceptions.total", "type", "Business").increment();

        log.info("Business exception: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        buildErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Business Rule Violation",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception) {
        meterRegistry.counter("exceptions.total", "type", "Generic").increment();

        log.error("Unexpected error occurred", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        buildErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleConflictException(ConflictException exception) {
        meterRegistry.counter("exceptions.total", "type", "Conflict").increment();

        log.warn("Conflict exception: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        buildErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        meterRegistry.counter("exceptions.total", "type", "DataIntegrityViolation").increment();

        log.warn("Data integrity violation (e.g. slot already taken): {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        buildErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                "Slot already taken for this therapist and time"
                        )
                );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    private static Map<String, Object> buildErrorResponse(int statusValue, String error, String exceptionMessage) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", statusValue,
                "error", error,
                "message", exceptionMessage
        );
    }
}
