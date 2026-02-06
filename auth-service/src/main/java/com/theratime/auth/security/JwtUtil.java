package com.theratime.auth.security;

import com.theratime.auth.config.JwtConfig;
import com.theratime.auth.entity.Credentials;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    private Key getKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }
    public String generateAccessToken(Credentials credentials) {
        return Jwts.builder()
                .setSubject(credentials.getId().toString())
                .claim("email", credentials.getEmail())
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiry()))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Credentials credentials) {
        return Jwts.builder()
                .setSubject(credentials.getId().toString())
                .claim("email", credentials.getEmail())
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiry()))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        Claims claim = parseToken(token);
        return "REFRESH".equals(claim.get("type"));
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
