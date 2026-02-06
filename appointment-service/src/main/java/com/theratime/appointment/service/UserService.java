package com.theratime.appointment.service;

import com.theratime.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceBaseUrl;

    @Retry(name = "userServiceLookup")
    @CircuitBreaker(name = "userServiceLookup")
    public boolean isUserPresent(Long userId) {
        getUserByIdOrThrow(userId);
        return true;
    }

    @Retry(name = "userServiceLookup")
    @CircuitBreaker(name = "userServiceLookup")
    public String getUserRole(Long userId) {
        Map<String, Object> user = getUserByIdOrThrow(userId);
        if (user != null && user.containsKey("role")) {
            Object role = user.get("role");
            return role != null ? role.toString() : null;
        }
        return null;
    }

    /**
     * Returns the tenant (practice) id for the user. Used for multi-tenant scoping.
     */
    @Retry(name = "userServiceLookup")
    @CircuitBreaker(name = "userServiceLookup")
    public Long getTenantId(Long userId) {
        Map<String, Object> user = getUserByIdOrThrow(userId);
        if (user != null && user.containsKey("tenantId") && user.get("tenantId") != null) {
            Object tid = user.get("tenantId");
            if (tid instanceof Number) {
                return ((Number) tid).longValue();
            }
        }
        return 1L;
    }

    private Map<String, Object> getUserByIdOrThrow(Long userId) {
        String url = userServiceBaseUrl + "/user/" + userId;
        try {
            Map<String, Object> user = restTemplate.getForObject(url, Map.class);
            if (user == null) {
                throw new ResourceNotFoundException("User with ID " + userId + " does not exist");
            }
            return user;
        } catch (RestClientResponseException e) {
            // Treat 4xx as "not found", let 5xx bubble up to be handled by retry/circuit breaker
            if (e.getStatusCode().is4xxClientError()) {
                throw new ResourceNotFoundException("User with ID " + userId + " does not exist");
            }
            throw e;
        } catch (RestClientException e) {
            // Network / other client issues: propagate so Resilience4j can apply retry/circuit breaker
            throw e;
        }
    }
}