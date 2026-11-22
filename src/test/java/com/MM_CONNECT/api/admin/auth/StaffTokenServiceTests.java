package com.MM_CONNECT.api.admin.auth;

import com.MM_CONNECT.api.auth.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StaffTokenServiceTests {
    private static final String SECRET = "TEST_ONLY_staff_signing_secret";
    private final StaffTokenService tokens = new StaffTokenService(SECRET, 15, Clock.systemUTC());
    @Test void wrongAudienceRefreshExpiredAndUnsignedTokensAreRejected() {
        for (String token : new String[] {
                token("wrong-app", "staff_access", Instant.now().plusSeconds(60)),
                token("mm-connect-admin", "refresh", Instant.now().plusSeconds(60)),
                token("mm-connect-admin", "staff_access", Instant.now().minusSeconds(60)),
                JWT.create().withIssuer("parinay-api").withAudience("mm-connect-admin")
                        .withClaim("token_type", "staff_access").sign(Algorithm.HMAC256(SECRET)),
                JWT.create().withIssuer("parinay-api").withAudience("mm-connect-admin")
                        .withClaim("token_type", "staff_access").sign(Algorithm.none())}) {
            assertThrows(JWTVerificationException.class, () -> tokens.verify(token));
        }
    }
    @Test void staffTokensContainOnlyTheRequiredIdentityAndVersionClaims() {
        User user = new User(); user.setId(UUID.randomUUID()); user.setRole("ROLE_ADMIN");
        StaffAccount staff = new StaffAccount(user.getId(), "test.admin", "TEST_ONLY_hash", Instant.now());
        var decoded = tokens.verify(tokens.issue(user, staff));
        assertEquals(user.getId().toString(), decoded.getSubject());
        assertEquals(900, decoded.getExpiresAtAsInstant().getEpochSecond() - decoded.getIssuedAtAsInstant().getEpochSecond());
        assertTrue(decoded.getClaim("password_hash").isMissing()); assertTrue(decoded.getClaim("firebase_uid").isMissing());
    }
    @Test void productionEncoderUsesSaltedOneWayHashesAndChecksTheWholePassword() {
        var encoder = new StaffSecurityConfig().staffPasswordEncoder();
        String raw = "TEST_ONLY_long_password";
        String first = encoder.encode(raw); String second = encoder.encode(raw);
        assertNotEquals(raw, first); assertNotEquals(first, second);
        assertTrue(encoder.matches(raw, first)); assertFalse(encoder.matches(raw + "x", first));
    }
    private String token(String audience, String type, Instant expiry) {
        return JWT.create().withIssuer("parinay-api").withAudience(audience)
                .withClaim("token_type", type).withSubject(UUID.randomUUID().toString())
                .withExpiresAt(expiry).sign(Algorithm.HMAC256(SECRET));
    }
}
