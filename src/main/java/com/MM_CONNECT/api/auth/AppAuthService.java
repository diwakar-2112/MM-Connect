package com.MM_CONNECT.api.auth;

import com.MM_CONNECT.api.auth.dto.TokenResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AppAuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse processFirebaseLogin(String firebaseTokenStr) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenStr);

            // Extract whatever Google gives us (could be email from Google Sign-In, or phone from OTP)
            String email = (String) decodedToken.getClaims().get("email");
            String phoneNumber = (String) decodedToken.getClaims().get("phone_number");

            boolean verifiedEmail = email != null && Boolean.TRUE.equals(decodedToken.getClaims().get("email_verified"));

            if (email == null && phoneNumber == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A verified Firebase identity is required.");
            }

            // Firebase UID is the stable identity; verified contact claims bind legacy accounts once.
            Optional<User> existingUser = userRepository.findByFirebaseUid(decodedToken.getUid());
            if (existingUser.isEmpty()) {
                if (verifiedEmail) existingUser = userRepository.findByEmail(email);
                else if (phoneNumber != null) existingUser = userRepository.findByPhoneNumber(phoneNumber);
                else throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A verified Firebase identity is required.");
            }

            boolean isNew = existingUser.isEmpty();

            // Create user if they don't exist
            User user = existingUser.orElseGet(() -> {
                User newUser = new User(phoneNumber, verifiedEmail ? email : null);
                newUser.setFirebaseUid(decodedToken.getUid());
                return userRepository.save(newUser);
            });

            if (!user.isActive() || user.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active.");
            }
            if (user.getFirebaseUid() == null
                    && com.MM_CONNECT.api.admin.StaffAccessService.isStaffRole(user.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff identity has not been provisioned.");
            }
            if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(decodedToken.getUid())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account identity does not match.");
            }
            user.setFirebaseUid(decodedToken.getUid());

            // Generate backend JWTs
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Reporting records successful backend session creation only. Never
            // infer historical login times from generic account updates.
            user.setLastLoginAt(Instant.now());
            Object claimedName = decodedToken.getClaims().get("name");
            if (claimedName instanceof String name && !name.isBlank()) {
                String displayName = name.strip();
                int end = displayName.offsetByCodePoints(0, Math.min(100,
                        displayName.codePointCount(0, displayName.length())));
                user.setSignInName(displayName.substring(0, end));
            }

            return new TokenResponse(accessToken, refreshToken, isNew);

        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase token");
        }
    }
}
