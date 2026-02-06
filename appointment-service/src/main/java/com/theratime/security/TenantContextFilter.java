package com.theratime.security;

import com.theratime.appointment.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sets the current tenant in TenantContext from the authenticated user
 * so TenantAwareDataSource can switch search_path to the tenant's schema.
 * Runs after TokenValidatorFilter; clears tenant after the request.
 */
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object details = auth.getDetails();
                if (details instanceof Long userId) {
                    Long tenantId = userService.getTenantId(userId);
                    TenantContext.setTenantId(tenantId);
                    MDC.put("tenantId", String.valueOf(tenantId));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }
}
