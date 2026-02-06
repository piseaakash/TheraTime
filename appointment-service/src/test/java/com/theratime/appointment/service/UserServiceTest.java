package com.theratime.appointment.service;

import com.theratime.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    void isUserPresent_whenUserExists_returnsTrue() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(eq("http://user-service/user/1"), eq(Map.class)))
                .thenReturn(Map.of("id", 1L, "email", "a@b.com"));

        assertThat(userService.isUserPresent(1L)).isTrue();
    }

    @Test
    void isUserPresent_whenUserNull_throwsResourceNotFound() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        assertThatThrownBy(() -> userService.isUserPresent(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void getUserRole_whenRolePresent_returnsRole() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", 1L, "role", "THERAPIST"));

        assertThat(userService.getUserRole(1L)).isEqualTo("THERAPIST");
    }

    @Test
    void getUserRole_whenRoleNull_returnsNull() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", 1L));

        assertThat(userService.getUserRole(1L)).isNull();
    }

    @Test
    void getTenantId_whenTenantIdPresent_returnsTenantId() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", 1L, "tenantId", 5L));

        assertThat(userService.getTenantId(1L)).isEqualTo(5L);
    }

    @Test
    void getTenantId_whenTenantIdMissing_returnsDefault() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("id", 1L));

        assertThat(userService.getTenantId(1L)).isEqualTo(1L);
    }

    @Test
    void getTenantId_when4xx_throwsResourceNotFound() {
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", "http://user-service");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientResponseException("Not found", 404, "Not Found", null, null, null));

        assertThatThrownBy(() -> userService.getTenantId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void getUserRole_roleKeyPresentButNull_returnsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("id", 1L, "role", null));

        assertThat(userService.getUserRole(1L)).isNull();
    }

    @Test
    void getTenantId_tenantIdNonNumeric_returnsDefault() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("id", 1L, "tenantId", "not-a-number"));

        assertThat(userService.getTenantId(1L)).isEqualTo(1L);
    }
}
