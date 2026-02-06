package com.theratime.auth.delegate;

import com.theratime.auth.model.*;
import com.theratime.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApiDelegateImplTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthApiDelegateImpl delegate;

    @Test
    void login_delegatesAndReturnsOk() {
        LoginRequest request = new LoginRequest().email("a@b.com").password("p");
        LoginResponse response = new LoginResponse().accessToken("at").refreshToken("rt").expiresIn(900);
        when(authService.login(request)).thenReturn(response);

        ResponseEntity<LoginResponse> result = delegate.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(authService).login(request);
    }

    @Test
    void logout_delegatesAndReturnsOk() {
        LogoutRequest request = new LogoutRequest().refreshToken("rt");
        ResponseEntity<Void> result = delegate.logout(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).logout(request);
    }

    @Test
    void refreshToken_delegatesAndReturnsOk() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("rt");
        LoginResponse response = new LoginResponse().accessToken("at").refreshToken("rt").expiresIn(900);
        when(authService.refreshToken(request)).thenReturn(response);

        ResponseEntity<LoginResponse> result = delegate.refreshToken(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(authService).refreshToken(request);
    }

    @Test
    void register_delegatesAndReturnsCreated() {
        RegisterRequest request = new RegisterRequest()
                .email("a@b.com")
                .password("p")
                .role(RegisterRequest.RoleEnum.THERAPIST)
                .firstName("F")
                .lastName("L");
        ResponseEntity<Void> result = delegate.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).register(request);
    }
}
