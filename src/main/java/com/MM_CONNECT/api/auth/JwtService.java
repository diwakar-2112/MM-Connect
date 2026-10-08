package com.MM_CONNECT.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {

    // Reads the secret from application.yml (we'll add it in the next step)
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.access-token-expiration-minutes}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    /**
     * Generates a short-lived Access Token (e.g., 15 minutes)
     */
    public String generateAccessToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("phone", user.getPhoneNumber())
                .withClaim("role", user.getRole())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(accessTokenExpiration, ChronoUnit.MINUTES))
                .withIssuer("parinay-api")
                .sign(algorithm);
    }

    /**
     * Generates a long-lived Refresh Token (e.g., 30 days)
     */
    public String generateRefreshToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withSubject(user.getId().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS))
                .withIssuer("parinay-api")
                .sign(algorithm);
    }

    /**
     * Validates and decodes the token
     */
    public DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.require(algorithm)
                .withIssuer("parinay-api")
                .build()
                .verify(token);
    }

    /**
     * Extracts User UUID from token subject
     */
    public UUID extractUserId(DecodedJWT decodedJWT) {
        return UUID.fromString(decodedJWT.getSubject());
    }
}