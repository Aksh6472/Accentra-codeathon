package com.accentra.leavemanagement.security;

import com.accentra.leavemanagement.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET is not set. Generate one with: openssl rand -base64 48");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("APP_JWT_SECRET must be Base64. Generate one with: openssl rand -base64 48", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be a Base64 value of at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = Duration.ofMinutes(properties.jwt().expirationMinutes());
        this.clock = clock;
    }

    public IssuedToken issue(Long userId, String email, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Returns the user id carried by a valid, unexpired token; empty for anything else. */
    public Optional<Long> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
