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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredentialsRepository credentialsRepository;

    private final RestTemplate restTemplate;

    private final JwtUtil jwtUtil;
    private final UserConfig userConfig;
    private final BCryptPasswordEncoder passwordEncoder;

    //Replace with Redis in large scale distributed application (after scaling)
    private final ConcurrentHashMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {

        Credentials credentials = credentialsRepository
                .findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if(!passwordEncoder.matches(
                loginRequest.getPassword(),
                credentials.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }


        String accessToken = jwtUtil.generateAccessToken(credentials);
        String refreshToken = jwtUtil.generateRefreshToken(credentials);

        credentials.setRefreshToken(refreshToken);
        credentials.setRefreshTokenExpiry(
                LocalDateTime.now().plusDays(7)
        );

        credentialsRepository.save(credentials);

        return new LoginResponse()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900);
    }

    @Transactional
    public void logout(LogoutRequest logoutRequest) {
        // Implement logout logic (e.g., invalidate tokens)
        int updated = credentialsRepository.clearRefreshToken(logoutRequest.getRefreshToken());

        if(updated == 0) {
            throw new InvalidRefreshTokenException("Invalid or already logged out token");
        }
    }

    @Transactional
    @Retry(name = "userServiceRegister")
    @CircuitBreaker(name = "userServiceRegister")
    public void register(RegisterRequest registerRequest) {
        String userServiceUrl = userConfig.getUrl() + "/user";
        UserDto userDto = mapRegisterRequestToUserDto(registerRequest);
        UserResponse userResponse = restTemplate.postForObject(userServiceUrl, userDto, UserResponse.class);

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        Credentials credentials = Credentials.builder()
                .email(registerRequest.getEmail())
                .passwordHash(hashedPassword)
                .userId(userResponse.getId())
                .build();
        credentialsRepository.save(credentials);
    }

    private UserDto mapRegisterRequestToUserDto(RegisterRequest registerRequest) {
        return UserDto.builder()
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phone(registerRequest.getPhone())
                .role(registerRequest.getRole().toString())
                .build();
    }
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String incomingRefreshToken = refreshTokenRequest.getRefreshToken();
        
        if (!jwtUtil.validateRefreshToken(incomingRefreshToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        Claims claims = jwtUtil.parseToken(incomingRefreshToken);
        Credentials credentials = credentialsRepository
                .findById(Long.parseLong(claims.getSubject()))
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        if (credentials.getRefreshToken() == null || !credentials.getRefreshToken().equals(incomingRefreshToken)) {
            throw new InvalidRefreshTokenException("Refresh token does not match stored token");
        }

        if (credentials.getRefreshTokenExpiry() == null || credentials.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        String newAccessToken = jwtUtil.generateAccessToken(credentials);
        String newRefreshToken = jwtUtil.generateRefreshToken(credentials);

        credentials.setRefreshToken(newRefreshToken);
        credentials.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        credentialsRepository.save(credentials);

        return new LoginResponse()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(900);
    }
}