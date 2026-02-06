package com.theratime.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private MeterRegistry meterRegistry;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(any(String.class), any(String.class), any(String.class))).thenReturn(counter);
        handler = new GlobalExceptionHandler(meterRegistry);
    }

    @Test
    void handleNotFound_returnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Appointment not found");
        ResponseEntity<Map<String, Object>> res = handler.handleNotFound(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsEntry("status", 404);
        assertThat(res.getBody()).containsEntry("error", "Not Found");
        assertThat(res.getBody()).containsEntry("message", "Appointment not found");
        verify(meterRegistry).counter(eq("exceptions.total"), eq("type"), eq("ResourceNotFound"));
    }

    @Test
    void handleBusiness_returnsBadRequest() {
        BusinessException ex = new BusinessException("Invalid time slot");
        ResponseEntity<Map<String, Object>> res = handler.handleBusiness(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("status", 400);
        assertThat(res.getBody()).containsEntry("error", "Business Rule Violation");
        assertThat(res.getBody()).containsEntry("message", "Invalid time slot");
        verify(meterRegistry).counter(eq("exceptions.total"), eq("type"), eq("Business"));
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected");
        ResponseEntity<Map<String, Object>> res = handler.handleGeneric(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).containsEntry("status", 500);
        assertThat(res.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(res.getBody()).containsEntry("message", "Unexpected");
        verify(meterRegistry).counter(eq("exceptions.total"), eq("type"), eq("Generic"));
    }

    @Test
    void handleConflictException_returnsConflict() {
        ConflictException ex = new ConflictException("Slot already taken");
        ResponseEntity<Map<String, Object>> res = handler.handleConflictException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).containsEntry("status", 409);
        assertThat(res.getBody()).containsEntry("error", "Conflict");
        assertThat(res.getBody()).containsEntry("message", "Slot already taken");
        verify(meterRegistry).counter(eq("exceptions.total"), eq("type"), eq("Conflict"));
    }

    @Test
    void handleDataIntegrityViolation_returnsConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key");
        ResponseEntity<Map<String, Object>> res = handler.handleDataIntegrityViolation(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).containsEntry("status", 409);
        assertThat(res.getBody()).containsEntry("message", "Slot already taken for this therapist and time");
        verify(meterRegistry).counter(eq("exceptions.total"), eq("type"), eq("DataIntegrityViolation"));
    }

    @Test
    void handleInvalidTokenException_returnsUnauthorized() {
        InvalidTokenException ex = new InvalidTokenException("Invalid token");
        ResponseEntity<String> res = handler.handleInvalidTokenException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isEqualTo("Invalid token");
    }
}
