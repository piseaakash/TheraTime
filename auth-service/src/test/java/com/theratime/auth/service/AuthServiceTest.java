package com.theratime.auth.service;

import com.theratime.auth.config.UserConfig;
import com.theratime.auth.dto.UserDto;
import com.theratime.auth.dto.UserResponse;
import com.theratime.auth.entity.Credentials;
import com.theratime.auth.exception.InvalidCredentialsException;
import com.theratime.auth.exception.InvalidRefreshTokenException;
import com.theratime.auth.model.*;
import com.theratime.auth.repository.CredentialsRepository;
import com.theratime.auth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialsRepository credentialsRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserConfig userConfig;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_returnsTokensAndPersistsRefreshToken() {
        LoginRequest request = new LoginRequest()
                .email("test@example.com")
                .password("password");

        Credentials creds = Credentials.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed")
                .build();

        when(credentialsRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(creds));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(creds)).thenReturn("access");
        when(jwtUtil.generateRefreshToken(creds)).thenReturn("refresh");

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getExpiresIn()).isEqualTo(900);

        verify(credentialsRepository).save(creds);
        assertThat(creds.getRefreshToken()).isEqualTo("refresh");
        assertThat(creds.getRefreshTokenExpiry()).isAfter(LocalDateTime.now());
    }

    @Test
    void login_withInvalidPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest()
                .email("test@example.com")
                .password("bad");

        Credentials creds = Credentials.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed")
                .build();

        when(credentialsRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(creds));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void register_createsUserViaUserServiceAndSavesCredentials() {
        RegisterRequest request = new RegisterRequest()
                .email("test@example.com")
                .password("secret")
                .role(RegisterRequest.RoleEnum.THERAPIST)
                .firstName("John")
                .lastName("Doe")
                .phone("123");

        when(userConfig.getUrl()).thenReturn("http://user-service");
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(99L);
        when(restTemplate.postForObject(eq("http://user-service/user"), any(UserDto.class), eq(UserResponse.class)))
                .thenReturn(userResponse);

        authService.register(request);

        ArgumentCaptor<Credentials> credsCaptor = ArgumentCaptor.forClass(Credentials.class);
        verify(credentialsRepository).save(credsCaptor.capture());
        Credentials saved = credsCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getUserId()).isEqualTo(99L);
    }

    @Test
    void refreshToken_withValidStoredToken_rotatesTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest()
                .refreshToken("oldRefresh");

        when(jwtUtil.validateRefreshToken("oldRefresh")).thenReturn(true);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.parseToken("oldRefresh")).thenReturn(claims);

        Credentials creds = Credentials.builder()
                .id(1L)
                .refreshToken("oldRefresh")
                .refreshTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();
        when(credentialsRepository.findById(1L)).thenReturn(Optional.of(creds));

        when(jwtUtil.generateAccessToken(creds)).thenReturn("newAccess");
        when(jwtUtil.generateRefreshToken(creds)).thenReturn("newRefresh");

        LoginResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("newAccess");
        assertThat(response.getRefreshToken()).isEqualTo("newRefresh");
        verify(credentialsRepository).save(creds);
        assertThat(creds.getRefreshToken()).isEqualTo("newRefresh");
    }

    @Test
    void refreshToken_withInvalidStoredToken_throwsInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest()
                .refreshToken("oldRefresh");

        when(jwtUtil.validateRefreshToken("oldRefresh")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.parseToken("oldRefresh")).thenReturn(claims);

        Credentials creds = Credentials.builder()
                .id(1L)
                .refreshToken("different")
                .refreshTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();
        when(credentialsRepository.findById(1L)).thenReturn(Optional.of(creds));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest()
                .email("nobody@example.com")
                .password("p");
        when(credentialsRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void logout_noRowsUpdated_throwsInvalidRefreshToken() {
        LogoutRequest request = new LogoutRequest().refreshToken("bad-token");
        when(credentialsRepository.clearRefreshToken("bad-token")).thenReturn(0);

        assertThatThrownBy(() -> authService.logout(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid or already logged out");
    }

    @Test
    void refreshToken_validationFails_throwsInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("invalid");
        when(jwtUtil.validateRefreshToken("invalid")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_userNotFound_throwsInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("rt");
        when(jwtUtil.validateRefreshToken("rt")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("999");
        when(jwtUtil.parseToken("rt")).thenReturn(claims);
        when(credentialsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshToken_nullRefreshToken_throwsInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("rt");
        when(jwtUtil.validateRefreshToken("rt")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.parseToken("rt")).thenReturn(claims);
        Credentials creds = Credentials.builder()
                .id(1L)
                .refreshToken(null)
                .refreshTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();
        when(credentialsRepository.findById(1L)).thenReturn(Optional.of(creds));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void refreshToken_expiredRefreshToken_throwsInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("rt");
        when(jwtUtil.validateRefreshToken("rt")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.parseToken("rt")).thenReturn(claims);
        Credentials creds = Credentials.builder()
                .id(1L)
                .refreshToken("rt")
                .refreshTokenExpiry(LocalDateTime.now().minusDays(1))
                .build();
        when(credentialsRepository.findById(1L)).thenReturn(Optional.of(creds));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }
}


