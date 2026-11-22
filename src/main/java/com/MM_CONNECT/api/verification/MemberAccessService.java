package com.MM_CONNECT.api.verification;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class MemberAccessService {
    private final UserRepository users;
    private final ProfileRepository profiles;

    public MemberAccessService(UserRepository users, ProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    public User activeUser(UUID userId) {
        return users.findById(userId).filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active."));
    }

    public boolean hasMemberAccess(User user) {
        return user.isVerified() && profiles.existsById(user.getId());
    }

    public User requireApproved(UUID userId) {
        User user = activeUser(userId);
        if (!profiles.existsById(userId)) {
            throw locked("ONBOARDING_REQUIRED", "Please finish onboarding to continue.");
        }
        if (!user.isVerified()) {
            throw locked("VERIFICATION_REQUIRED", "Your profile needs approval before you can continue.");
        }
        return user;
    }

    public User requirePublishedTarget(UUID targetId) {
        return users.findById(targetId)
                .filter(u -> u.isActive() && u.getDeletedAt() == null && u.isVerified())
                .filter(u -> profiles.existsById(targetId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    private ResponseStatusException locked(String code, String detail) {
        var error = new ResponseStatusException(HttpStatus.FORBIDDEN, detail);
        error.getBody().setProperty("code", code);
        return error;
    }
}
