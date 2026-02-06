package com.theratime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-at-least-256-bits-long-for-hs256-algorithm";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void validateAccessToken_withNullOrEmpty_returnsFalse() {
        assertThat(jwtUtil.validateAccessToken(null)).isFalse();
        assertThat(jwtUtil.validateAccessToken("")).isFalse();
    }

    @Test
    void validateAccessToken_withValidToken_returnsTrue() {
        String token = createValidToken(1L, "user@example.com");
        assertThat(jwtUtil.validateAccessToken(token)).isTrue();
    }

    @Test
    void getClaimsFromToken_withValidToken_returnsClaims() {
        String token = createValidToken(99L, "test@example.com");
        Claims claims = jwtUtil.getClaimsFromToken(token);
        assertThat(claims.getSubject()).isEqualTo("99");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    }

    @Test
    void getClaimsFromToken_withInvalidToken_throws() {
        assertThatThrownBy(() -> jwtUtil.getClaimsFromToken("invalid"))
                .isInstanceOf(Exception.class);
    }

    private String createValidToken(Long userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .signWith(key)
                .compact();
    }
}
