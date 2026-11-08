package com.MM_CONNECT.api.connection;

import com.MM_CONNECT.api.auth.User;
import com.MM_CONNECT.api.auth.UserRepository;
import com.MM_CONNECT.api.profile.Profile;
import com.MM_CONNECT.api.profile.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ConnectionController {

    private final ConnectionRequestRepository connectionRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ConnectionController(ConnectionRequestRepository connectionRepository,
                                ProfileRepository profileRepository,
                                UserRepository userRepository) {
        this.connectionRepository = connectionRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    // --- DTOs ---
    public record SendNotePayload(String note) {}

    public record RequestItemDto(
            UUID requestId,
            UUID otherUserId,
            String otherUserName,
            String otherUserCity,
            String status,
            String note,
            Instant createdAt
    ) {}

    public record ContactRevealDto(
            UUID connectionId,
            UUID partnerUserId,
            String fullName,
            String phoneNumber,
            String email
    ) {}

    // 1. SEND CONNECTION REQUEST
    @PostMapping("/requests/{targetUserId}")
    @Transactional
    public ResponseEntity<RequestItemDto> sendRequest(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID targetUserId,
            @RequestBody(required = false) SendNotePayload payload) {

        if (myUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a request to yourself.");
        }

        if (connectionRepository.existsBySenderIdAndReceiverId(myUserId, targetUserId) ||
                connectionRepository.existsBySenderIdAndReceiverId(targetUserId, myUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A connection request already exists between you two.");
        }

        String note = (payload != null) ? payload.note() : null;
        ConnectionRequest saved = connectionRepository.save(new ConnectionRequest(myUserId, targetUserId, note));

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved, targetUserId));
    }

    // 2. VIEW RECEIVED REQUESTS
    @GetMapping("/requests/received")
    public ResponseEntity<List<RequestItemDto>> getReceivedRequests(@AuthenticationPrincipal UUID myUserId) {
        List<ConnectionRequest> list = connectionRepository.findByReceiverIdOrderByCreatedAtDesc(myUserId);
        List<RequestItemDto> result = new ArrayList<>();
        for (ConnectionRequest req : list) {
            result.add(toDto(req, req.getSenderId()));
        }
        return ResponseEntity.ok(result);
    }

    // 3. VIEW SENT REQUESTS
    @GetMapping("/requests/sent")
    public ResponseEntity<List<RequestItemDto>> getSentRequests(@AuthenticationPrincipal UUID myUserId) {
        List<ConnectionRequest> list = connectionRepository.findBySenderIdOrderByCreatedAtDesc(myUserId);
        List<RequestItemDto> result = new ArrayList<>();
        for (ConnectionRequest req : list) {
            result.add(toDto(req, req.getReceiverId()));
        }
        return ResponseEntity.ok(result);
    }

    // 4. ACCEPT REQUEST (Only Receiver can accept)
    @PostMapping("/requests/{requestId}/accept")
    @Transactional
    public ResponseEntity<RequestItemDto> acceptRequest(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID requestId) {

        ConnectionRequest req = getRequestOrThrow(requestId);
        if (!req.getReceiverId().equals(myUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can accept this request.");
        }
        if (!"SENT".equals(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is no longer in SENT state.");
        }

        req.setStatus("ACCEPTED");
        connectionRepository.save(req);
        return ResponseEntity.ok(toDto(req, req.getSenderId()));
    }

    // 5. DECLINE REQUEST (Only Receiver can decline)
    @PostMapping("/requests/{requestId}/decline")
    @Transactional
    public ResponseEntity<RequestItemDto> declineRequest(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID requestId) {

        ConnectionRequest req = getRequestOrThrow(requestId);
        if (!req.getReceiverId().equals(myUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can decline this request.");
        }

        req.setStatus("DECLINED");
        connectionRepository.save(req);
        return ResponseEntity.ok(toDto(req, req.getSenderId()));
    }

    // 6. WITHDRAW REQUEST (Only Sender can cancel before response)
    @PostMapping("/requests/{requestId}/withdraw")
    @Transactional
    public ResponseEntity<RequestItemDto> withdrawRequest(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID requestId) {

        ConnectionRequest req = getRequestOrThrow(requestId);
        if (!req.getSenderId().equals(myUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can withdraw this request.");
        }
        if (!"SENT".equals(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot withdraw once already responded to.");
        }

        req.setStatus("WITHDRAWN");
        connectionRepository.save(req);
        return ResponseEntity.ok(toDto(req, req.getReceiverId()));
    }

    // 7. THE CONTACT REVEAL GUARD (Strict Tier-2 Privacy)
    @GetMapping("/connections/{requestId}/contact")
    public ResponseEntity<ContactRevealDto> getPartnerContact(
            @AuthenticationPrincipal UUID myUserId,
            @PathVariable UUID requestId) {

        ConnectionRequest req = getRequestOrThrow(requestId);

        boolean isParticipant = req.getSenderId().equals(myUserId) || req.getReceiverId().equals(myUserId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this connection.");
        }

        if (!"ACCEPTED".equals(req.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Contact details are locked until the request is ACCEPTED."
            );
        }

        UUID partnerId = req.getSenderId().equals(myUserId) ? req.getReceiverId() : req.getSenderId();

        User partnerUser = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Profile partnerProfile = profileRepository.findById(partnerId).orElse(null);
        String name = (partnerProfile != null) ? partnerProfile.getFullName() : "Member";

        return ResponseEntity.ok(new ContactRevealDto(
                req.getId(),
                partnerId,
                name,
                partnerUser.getPhoneNumber(),
                partnerUser.getEmail()
        ));
    }

    private ConnectionRequest getRequestOrThrow(UUID requestId) {
        return connectionRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    private RequestItemDto toDto(ConnectionRequest req, UUID otherUserId) {
        Profile otherProfile = profileRepository.findById(otherUserId).orElse(null);
        String name = (otherProfile != null) ? otherProfile.getFullName() : "Unknown";
        String city = (otherProfile != null) ? otherProfile.getCurrentCity() : "";

        return new RequestItemDto(
                req.getId(),
                otherUserId,
                name,
                city,
                req.getStatus(),
                req.getNote(),
                req.getCreatedAt()
        );
    }
}