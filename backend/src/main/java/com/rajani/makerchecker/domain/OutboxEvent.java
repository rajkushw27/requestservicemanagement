package com.rajani.makerchecker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.Instant;

/** Transactional outbox row. OutboxRelay delivers these at-least-once. */
@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String approvalRequestId;
    private String eventType;

    @Lob
    private String payload;

    private String callbackUrl;

    private boolean delivered = false;
    private int attempts = 0;
    private String lastError;

    private Instant createdAt = Instant.now();
    private Instant deliveredAt;

    public static OutboxEvent of(String approvalRequestId, String eventType, String payload, String callbackUrl) {
        OutboxEvent e = new OutboxEvent();
        e.approvalRequestId = approvalRequestId;
        e.eventType = eventType;
        e.payload = payload;
        e.callbackUrl = callbackUrl;
        return e;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
