package com.MM_CONNECT.api.connection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {

    List<ConnectionRequest> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    List<ConnectionRequest> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    boolean existsBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, String status);
}
