package com.workproofpay.backend.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String BASE64_PREFIX = "base64:";
    private static final String BASE64_URL_PREFIX = "base64url:";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = resolveSecretBytes(secret == null ? "" : secret.trim());
        if (keyBytes.length < 32) {
            byte[] expanded = new byte[32];
            System.arraycopy(keyBytes, 0, expanded, 0, keyBytes.length);
            keyBytes = expanded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes(String value) {
        if (value.startsWith(BASE64_PREFIX)) {
            return Decoders.BASE64.decode(value.substring(BASE64_PREFIX.length()));
        }
        if (value.startsWith(BASE64_URL_PREFIX)) {
            return Decoders.BASE64URL.decode(value.substring(BASE64_URL_PREFIX.length()));
        }
        // Default: treat jwt.secret as plain text.
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public String createAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessExpirationMs);

        return Jwts.builder()
                .subject("AccessToken")
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public AccessTokenPayload parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number userIdNum = claims.get(CLAIM_USER_ID, Number.class);
        String email = claims.get(CLAIM_EMAIL, String.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        return new AccessTokenPayload(userIdNum.longValue(), email, role);
    }

    public String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String trimmed = authHeader.trim();
        if (trimmed.toLowerCase().startsWith("bearer ")) {
            return trimmed.substring(7).trim();
        }
        return null;
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    public record AccessTokenPayload(Long userId, String email, String role) {
    }
}
