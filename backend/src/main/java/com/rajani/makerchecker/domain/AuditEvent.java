package com.rajani.makerchecker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.Instant;

/** Append-only. Never add an update or delete path here. */
@Entity
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String approvalRequestId;
    private String tenantId;
    private String actor;
    private String action;
    private String fromStatus;
    private String toStatus;

    @Lob
    private String detail;

    private Instant createdAt = Instant.now();

    public static AuditEvent of(String approvalRequestId, String tenantId, String actor, String action,
                                 String fromStatus, String toStatus, String detail) {
        AuditEvent e = new AuditEvent();
        e.approvalRequestId = approvalRequestId;
        e.tenantId = tenantId;
        e.actor = actor;
        e.action = action;
        e.fromStatus = fromStatus;
        e.toStatus = toStatus;
        e.detail = detail;
        return e;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
