package com.MM_CONNECT.api.media;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.connection.ConnectionRequestRepository;
import com.MM_CONNECT.api.matching.UserInteractionRepository;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class PhotoAccessService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final ProfilePhotoRepository photos;
    private final UserInteractionRepository interactions;
    private final ConnectionRequestRepository connections;

    public PhotoAccessService(UserRepository users, ProfileRepository profiles, ProfilePhotoRepository photos,
                              UserInteractionRepository interactions, ConnectionRequestRepository connections) {
        this.users = users;
        this.profiles = profiles;
        this.photos = photos;
        this.interactions = interactions;
        this.connections = connections;
    }

    public void requireView(UUID viewerId, String key) {
        ProfilePhoto photo = photos.findFirstByStorageKey(key)
                .orElseThrow(this::notFound);
        if (!canView(viewerId, photo)) throw notFound();
    }

    public boolean canView(UUID viewerId, ProfilePhoto photo) {
        User viewer = users.findById(viewerId).filter(this::active).orElse(null);
        if (viewer == null) return false;
        if (viewerId.equals(photo.getUserId())) return true;
        if (!viewer.isVerified() || !profiles.existsById(viewerId)) return false;
        User target = users.findById(photo.getUserId()).filter(this::active).orElse(null);
        if (target == null || !target.isVerified() || !profiles.existsById(target.getId())) return false;
        if (interactions.existsByActorIdAndTargetIdAndAction(viewerId, target.getId(), "BLOCKED")
                || interactions.existsByActorIdAndTargetIdAndAction(target.getId(), viewerId, "BLOCKED")) return false;
        if (!"APPROVED".equals(photo.getModerationStatus())) return false;
        return switch (photo.getVisibility()) {
            case "ALL_VERIFIED" -> true;
            case "ACCEPTED_ONLY" -> connections.existsBySenderIdAndReceiverIdAndStatus(viewerId, target.getId(), "ACCEPTED")
                    || connections.existsBySenderIdAndReceiverIdAndStatus(target.getId(), viewerId, "ACCEPTED");
            default -> false;
        };
    }

    private boolean active(User user) { return user.isActive() && user.getDeletedAt() == null; }
    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
    }
}
