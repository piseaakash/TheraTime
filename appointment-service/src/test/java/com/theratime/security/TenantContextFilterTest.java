package com.theratime.security;

import com.theratime.appointment.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TenantContextFilter filter;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_whenAuthNull_clearsContextAndContinues() throws Exception {
        SecurityContextHolder.clearContext();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    void doFilterInternal_whenNotAuthenticated_continuesWithoutSettingTenant() throws Exception {
        var auth = new TestingAuthenticationToken("u", "p");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userService, never()).getTenantId(any());
    }

    @Test
    void doFilterInternal_whenDetailsNotLong_continuesWithoutSettingTenant() throws Exception {
        var auth = new TestingAuthenticationToken("u", "p");
        auth.setAuthenticated(true);
        auth.setDetails("not-long");
        SecurityContextHolder.getContext().setAuthentication(auth);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userService, never()).getTenantId(any());
    }

    @Test
    void doFilterInternal_whenAuthenticatedWithUserId_setsTenantAndClears() throws Exception {
        var auth = new TestingAuthenticationToken("u", "p");
        auth.setAuthenticated(true);
        auth.setDetails(10L);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userService.getTenantId(10L)).thenReturn(5L);

        filter.doFilterInternal(request, response, filterChain);

        verify(userService).getTenantId(10L);
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }
}
