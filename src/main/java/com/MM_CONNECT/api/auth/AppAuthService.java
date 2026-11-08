package com.MM_CONNECT.api.auth;

import com.MM_CONNECT.api.auth.dto.TokenResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

            if (email == null && phoneNumber == null) {
                throw new RuntimeException("Authentication failed: No email or phone found in token");
            }

            // Find by Email OR Phone
            Optional<User> existingUser;
            if (email != null) {
                existingUser = userRepository.findByEmail(email);
            } else {
                existingUser = userRepository.findByPhoneNumber(phoneNumber);
            }

            boolean isNew = existingUser.isEmpty();

            // Create user if they don't exist
            User user = existingUser.orElseGet(() -> {
                User newUser = new User(phoneNumber, email);
                return userRepository.save(newUser);
            });

            // Generate backend JWTs
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return new TokenResponse(accessToken, refreshToken, isNew);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Invalid or expired Firebase token");
        }
    }
}