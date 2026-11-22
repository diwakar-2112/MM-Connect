package com.MM_CONNECT.api.admin;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** Fixed permissions for the first admin API release. Never trust a client role. */
@Service
public class StaffAccessService {
    private final UserRepository users;

    public StaffAccessService(UserRepository users) { this.users = users; }

    public User requireReviewer(UUID actorId) {
        User actor = users.findById(actorId)
                .filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(StaffAccessService::forbidden);
        if (!isStaffRole(actor.getRole())) throw forbidden();
        return actor;
    }

    public User requireSuperadmin(UUID actorId) {
        User actor = requireReviewer(actorId);
        if (!"ROLE_SUPERADMIN".equals(actor.getRole())) throw forbidden();
        return actor;
    }

    public User requireReportReader(UUID actorId) {
        return requireReviewer(actorId);
    }

    public static boolean isStaffRole(String role) {
        return "ROLE_ADMIN".equals(role) || "ROLE_SUPERADMIN".equals(role);
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff permission required.");
    }
}
