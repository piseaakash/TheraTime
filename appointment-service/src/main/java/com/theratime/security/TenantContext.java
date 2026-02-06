package com.theratime.security;

import com.theratime.exception.InvalidTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Holds the current request's tenant (schema-per-tenant) and user id.
 * Used by TenantAwareDataSource to set search_path and by services for validation.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final long DEFAULT_TENANT_ID = 1L;

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Current tenant id, or default (1) when not set (e.g. health checks).
     */
    public static Long getTenantId() {
        Long id = TENANT_ID.get();
        return id != null ? id : DEFAULT_TENANT_ID;
    }

    /**
     * Schema name for the current tenant (e.g. tenant_1, tenant_2).
     */
    public static String getCurrentSchema() {
        return "tenant_" + getTenantId();
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    /**
     * Returns the current user id from the JWT (set by TokenValidatorFilter).
     *
     * @throws InvalidTokenException if not authenticated or user id missing
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getDetails() == null) {
            throw new InvalidTokenException("Not authenticated");
        }
        Object details = auth.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        throw new InvalidTokenException("User id not found in security context");
    }
}
