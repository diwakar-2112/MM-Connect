package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.auth.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Clock;
import java.time.Instant;

/** Deliberately distinct from member access and refresh tokens. */
public class StaffTokenService {
    private final Algorithm algorithm;
    private final long expiresIn;
    private final Clock clock;
    public StaffTokenService(String secret, long expirationMinutes, Clock clock) {
        if (expirationMinutes < 1 || expirationMinutes > 60) throw new IllegalArgumentException("Staff token TTL must be 1-60 minutes.");
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiresIn = expirationMinutes * 60;
        this.clock = clock;
    }
    public String issue(User user, StaffAccount staff) {
        Instant now = clock.instant();
        return JWT.create().withIssuer("parinay-api").withAudience("mm-connect-admin")
                .withSubject(user.getId().toString()).withClaim("token_type", "staff_access")
                .withClaim("role", user.getRole()).withClaim("auth_version", staff.getAuthVersion())
                .withIssuedAt(now).withExpiresAt(now.plusSeconds(expiresIn)).sign(algorithm);
    }
    public DecodedJWT verify(String token) {
        DecodedJWT verified = JWT.require(algorithm).withIssuer("parinay-api").withAudience("mm-connect-admin")
                .withClaim("token_type", "staff_access").build().verify(token);
        if (verified.getExpiresAtAsInstant() == null) throw new JWTVerificationException("Staff token expiry is required.");
        return verified;
    }
    public long expiresIn() { return expiresIn; }
}
