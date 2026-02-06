package com.theratime.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTokenExpiredException_returnsUnauthorized() {
        TokenExpiredException ex = new TokenExpiredException("Token expired");
        ResponseEntity<Map<String, Object>> res = handler.handleTokenExpiredException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("status", 401);
        assertThat(res.getBody()).containsEntry("error", "Unauthorized");
        assertThat(res.getBody()).containsEntry("message", "Token expired");
        assertThat(res.getBody()).containsKey("timestamp");
    }

    @Test
    void handleInvalidCredentialsException_returnsUnauthorized() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid credentials");
        ResponseEntity<Map<String, Object>> res = handler.handleInvalidCredentialsException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("status", 401);
        assertThat(res.getBody()).containsEntry("message", "Invalid credentials");
    }

    @Test
    void handleInvalidRefreshTokenException_returnsUnauthorized() {
        InvalidRefreshTokenException ex = new InvalidRefreshTokenException("Invalid refresh token");
        ResponseEntity<Map<String, Object>> res = handler.handleInvalidRefreshTokenException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).containsEntry("status", 401);
        assertThat(res.getBody()).containsEntry("message", "Invalid refresh token");
    }

    @Test
    void handleGenericException_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected");
        ResponseEntity<Map<String, Object>> res = handler.handleGenericException(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).containsEntry("status", 500);
        assertThat(res.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(res.getBody()).containsEntry("message", "An unexpected error occurred");
    }
}
