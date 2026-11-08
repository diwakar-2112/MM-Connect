package com.MM_CONNECT.api.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class ConnectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false, updatable = false)
    private UUID receiverId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "SENT"; // SENT, ACCEPTED, DECLINED, WITHDRAWN

    @Column(name = "note", length = 250)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ConnectionRequest() {}

    public ConnectionRequest(UUID senderId, UUID receiverId, String note) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.note = note;
        this.status = "SENT";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getReceiverId() { return receiverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getNote() { return note; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}