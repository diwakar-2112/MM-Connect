package com.MM_CONNECT.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Configuration
public class FirebaseSetupConfig {

    // Spring organically loads the file from the resources folder
    @Value("classpath:firebase-admin.json")
    private Resource firebaseSecretFile;

    @PostConstruct
    public void prepareFirebaseEnvironment() {
        try {
            // Guard against double-initialization if the server restarts unexpectedly
            if (FirebaseApp.getApps().isEmpty()) {
                try (InputStream keyStream = firebaseSecretFile.getInputStream()) {

                    FirebaseOptions platformOptions = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(keyStream))
                            .build();

                    FirebaseApp.initializeApp(platformOptions);
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Critical error: Failed to bridge MM-CONNECT to Firebase", error);
        }
    }
}