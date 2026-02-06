package com.theratime.security;

import com.theratime.appointment.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidatorFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenValidatorFilter filter;

    @Test
    void doFilterInternal_whenNoAuthorizationHeader_writesUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("Missing or invalid Authorization header");
    }

    @Test
    void doFilterInternal_whenAuthorizationNotBearer_writesUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic xyz");
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("Missing or invalid Authorization header");
    }

    @Test
    void doFilterInternal_whenTokenInvalid_writesUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtUtil.validateAccessToken("bad-token")).thenReturn(false);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("Invalid or expired token");
    }

    @Test
    void doFilterInternal_whenUserRoleNull_writesUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");
        when(jwtUtil.validateAccessToken("valid")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("email", String.class)).thenReturn("a@b.com");
        when(jwtUtil.getClaimsFromToken("valid")).thenReturn(claims);
        when(userService.getUserRole(1L)).thenReturn(null);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("User role not found");
    }

    @Test
    void doFilterInternal_whenValidToken_setsAuthAndContinues() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");
        when(jwtUtil.validateAccessToken("valid")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("email", String.class)).thenReturn("a@b.com");
        when(jwtUtil.getClaimsFromToken("valid")).thenReturn(claims);
        when(userService.getUserRole(1L)).thenReturn("THERAPIST");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails()).isEqualTo(1L);
    }

    @Test
    void doFilterInternal_whenClaimsThrow_writesUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");
        when(jwtUtil.validateAccessToken("valid")).thenReturn(true);
        when(jwtUtil.getClaimsFromToken("valid")).thenThrow(new RuntimeException("parse error"));
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("Failed to authenticate");
    }
}
