package com.theratime.auth.delegate;

import com.theratime.auth.api.AuthApiDelegate;
import com.theratime.auth.model.*;
import com.theratime.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthApiDelegateImpl implements AuthApiDelegate {

    private final AuthService authService;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @Override
    public ResponseEntity<Void> logout(LogoutRequest logoutRequest) {
        // Delegate the logout logic to AuthService
        authService.logout(logoutRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        LoginResponse loginResponse = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @Override
    public ResponseEntity<Void> register(RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(201).build();
    }
}