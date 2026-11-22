package com.MM_CONNECT.api.admin;

import com.MM_CONNECT.api.verification.ReviewStatus;
import com.MM_CONNECT.api.verification.VerificationDtos;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
public class AdminVerificationController {
    private final AdminVerificationService verification;
    private final StaffAccessService access;

    public AdminVerificationController(AdminVerificationService verification, StaffAccessService access) {
        this.verification = verification;
        this.access = access;
    }

    public record StaffSession(UUID userId, String role, List<String> permissions) {}
    public record ReviewPage(List<VerificationDtos.QueueItem> content, int pageNumber, int pageSize,
                             long totalElements, int totalPages, boolean isLast) {}

    @GetMapping("/me")
    public ResponseEntity<StaffSession> me(@AuthenticationPrincipal UUID actorId) {
        var staff = access.requireReviewer(actorId);
        return privateResponse(new StaffSession(actorId, staff.getRole(), List.of("VERIFICATION_READ", "VERIFICATION_DECIDE", "REPORTS_READ")));
    }

    @GetMapping("/verifications")
    public ResponseEntity<ReviewPage> queue(@AuthenticationPrincipal UUID actorId,
            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<VerificationDtos.QueueItem> result = verification.queue(actorId, status, page, size);
        return privateResponse(new ReviewPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast()));
    }

    @GetMapping("/verifications/{reviewId}")
    public ResponseEntity<VerificationDtos.ReviewDetail> detail(@AuthenticationPrincipal UUID actorId,
                                                               @PathVariable UUID reviewId) {
        return privateResponse(verification.detail(actorId, reviewId));
    }

    @PostMapping("/verifications/{reviewId}/approve")
    public ResponseEntity<VerificationDtos.QueueItem> approve(@AuthenticationPrincipal UUID actorId,
            @PathVariable UUID reviewId, @Valid @RequestBody VerificationDtos.Approve request) {
        return privateResponse(verification.decide(actorId, reviewId, request.expectedVersion(), ReviewStatus.APPROVED, null));
    }

    @PostMapping("/verifications/{reviewId}/request-changes")
    public ResponseEntity<VerificationDtos.QueueItem> requestChanges(@AuthenticationPrincipal UUID actorId,
            @PathVariable UUID reviewId, @Valid @RequestBody VerificationDtos.RequestChanges request) {
        return privateResponse(verification.decide(actorId, reviewId, request.expectedVersion(),
                ReviewStatus.CHANGES_REQUIRED, request.feedback()));
    }

    private <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
    }
}
