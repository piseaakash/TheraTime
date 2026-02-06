package com.theratime.user.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUserNotFound_returnsNotFound() {
        UserNotFoundException ex = new UserNotFoundException("User not found with id: 1");
        ResponseEntity<Map<String, Object>> res = handler.handleUserNotFound(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsEntry("status", 404);
        assertThat(res.getBody()).containsEntry("error", "User Not Found");
        assertThat(res.getBody()).containsEntry("message", "User not found with id: 1");
        assertThat(res.getBody()).containsKey("timestamp");
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected");
        ResponseEntity<Map<String, Object>> res = handler.handleGeneric(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).containsEntry("status", 500);
        assertThat(res.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(res.getBody()).containsEntry("message", "Unexpected");
    }
}
