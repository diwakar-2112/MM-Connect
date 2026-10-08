package com.MM_CONNECT.api.auth;

import com.MM_CONNECT.api.auth.dto.TokenResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            // 1. Ask Google if the token is valid (this requires Firebase config, which we will add next)
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenStr);
            String phoneNumber = decodedToken.getClaims().get("phone_number").toString();

            // 2. Find existing user, or flag as new if they don't exist
            boolean isNew = !userRepository.existsByPhoneNumber(phoneNumber);

            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseGet(() -> {
                        // Create brand new user in the database
                        User newUser = new User(phoneNumber, null);
                        newUser.setVerified(false);
                        return userRepository.save(newUser);
                    });

            // 3. Generate our own backend JWTs
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // (Later: we will save the refreshToken into the refresh_tokens table here)

            return new TokenResponse(accessToken, refreshToken, isNew);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Invalid or expired Firebase token");
        }
    }
}