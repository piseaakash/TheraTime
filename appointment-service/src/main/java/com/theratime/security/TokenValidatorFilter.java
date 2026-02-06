package com.theratime.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.theratime.appointment.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
public class TokenValidatorFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateAccessToken(token)) {
            writeUnauthorizedError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired token");
            return;
        }

        try {
            Claims claims = jwtUtil.getClaimsFromToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);

            String role = userService.getUserRole(userId);
            if (role == null) {
                writeUnauthorizedError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "User role not found");
                return;
            }

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Populate MDC for observability
            MDC.put("userId", String.valueOf(userId));
            MDC.put("userEmail", email);
            MDC.put("userRole", role);

            String correlationId = request.getHeader("X-Correlation-Id");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put("correlationId", correlationId);
            response.setHeader("X-Correlation-Id", correlationId);
        } catch (Exception e) {
            writeUnauthorizedError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Failed to authenticate user");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedError(HttpServletResponse response,
                                        HttpStatus status,
                                        String error,
                                        String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", error,
                "message", message
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
