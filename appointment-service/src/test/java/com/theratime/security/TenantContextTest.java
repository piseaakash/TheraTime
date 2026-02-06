package com.theratime.security;

import com.theratime.exception.InvalidTokenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTenantId_whenNotSet_returnsDefault() {
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    void setTenantId_andGetTenantId_returnsSetValue() {
        TenantContext.setTenantId(5L);
        assertThat(TenantContext.getTenantId()).isEqualTo(5L);
    }

    @Test
    void clear_removesTenantId() {
        TenantContext.setTenantId(5L);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    void getCurrentSchema_returnsTenantPrefixed() {
        TenantContext.setTenantId(3L);
        assertThat(TenantContext.getCurrentSchema()).isEqualTo("tenant_3");
    }

    @Test
    void getCurrentSchema_whenNotSet_usesDefaultTenant() {
        assertThat(TenantContext.getCurrentSchema()).isEqualTo("tenant_1");
    }

    @Test
    void getCurrentUserId_whenAuthNull_throwsInvalidTokenException() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(TenantContext::getCurrentUserId)
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_throwsInvalidTokenException() {
        var auth = new TestingAuthenticationToken("user", "pass");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::getCurrentUserId)
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    void getCurrentUserId_whenDetailsNull_throwsInvalidTokenException() {
        var auth = new TestingAuthenticationToken("user", "pass");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::getCurrentUserId)
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    void getCurrentUserId_whenDetailsNotLong_throwsInvalidTokenException() {
        var auth = new TestingAuthenticationToken("user", "pass");
        auth.setAuthenticated(true);
        auth.setDetails("not-a-long");
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::getCurrentUserId)
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("User id not found");
    }

    @Test
    void getCurrentUserId_whenDetailsIsLong_returnsUserId() {
        var auth = new TestingAuthenticationToken("user", "pass");
        auth.setAuthenticated(true);
        auth.setDetails(42L);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(TenantContext.getCurrentUserId()).isEqualTo(42L);
    }
}
