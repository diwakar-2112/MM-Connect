package com.MM_CONNECT.api.admin.auth;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StaffAuthDtos {
    private StaffAuthDtos() {}
    public record Login(@NotBlank @Size(max = 64) String username,
                        @NotNull @Size(min = 1, max = 128) String password) {
        @Override public String toString() { return "StaffLogin[redacted]"; }
    }
    public record ChangePassword(@NotNull @Size(min = 1, max = 128) String currentPassword,
                                 @NotBlank @Size(min = 15, max = 128) String newPassword) {
        @Override public String toString() { return "StaffPasswordChange[redacted]"; }
    }
    public record CreateAdmin(@NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,63}") String username,
                              @NotBlank @Size(min = 15, max = 128) String temporaryPassword) {
        @Override public String toString() { return "StaffCreate[redacted]"; }
    }
    public record ResetPassword(@NotBlank @Size(min = 15, max = 128) String temporaryPassword) {
        @Override public String toString() { return "StaffReset[redacted]"; }
    }
    public record Me(UUID userId, String username, String role, boolean passwordChangeRequired,
                     List<String> permissions) {}
    public record Session(String accessToken, String tokenType, long expiresIn,
                          boolean passwordChangeRequired, Me staff) {
        @Override public String toString() { return "StaffSession[redacted]"; }
    }
    public record StaffMember(UUID userId, String username, String role, boolean active,
                              boolean passwordChangeRequired, Instant createdAt) {}
    public record StaffPage(List<StaffMember> content, int pageNumber, int pageSize,
                            long totalElements, int totalPages, boolean isLast) {}
}
