package com.rajani.makerchecker.domain;

import com.rajani.makerchecker.domain.Enums.ApprovalMode;
import com.rajani.makerchecker.domain.Enums.Operation;
import com.rajani.makerchecker.domain.Enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Caller-supplied idempotency key, unique per tenant. */
    private String requestId;
    private String tenantId;

    private String entityType;
    private String entityId;

    @Enumerated(EnumType.STRING)
    private Operation operation;

    private String summary;

    @Lob
    private String payloadBefore;
    @Lob
    private String payloadAfter;

    private BigDecimal amount;

    private String makerId;
    private String policyKey;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private int requiredApprovals;

    @Enumerated(EnumType.STRING)
    private ApprovalMode mode;

    /** Comma-separated, frozen at submit time. */
    @Column(length = 2000)
    private String approverChain;

    private Instant deadlineAt;
    private Instant decidedAt;
    private String callbackUrl;

    private Instant createdAt = Instant.now();

    @Version
    private long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPayloadBefore() { return payloadBefore; }
    public void setPayloadBefore(String payloadBefore) { this.payloadBefore = payloadBefore; }
    public String getPayloadAfter() { return payloadAfter; }
    public void setPayloadAfter(String payloadAfter) { this.payloadAfter = payloadAfter; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMakerId() { return makerId; }
    public void setMakerId(String makerId) { this.makerId = makerId; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public int getRequiredApprovals() { return requiredApprovals; }
    public void setRequiredApprovals(int requiredApprovals) { this.requiredApprovals = requiredApprovals; }
    public ApprovalMode getMode() { return mode; }
    public void setMode(ApprovalMode mode) { this.mode = mode; }
    public String getApproverChain() { return approverChain; }
    public void setApproverChain(String approverChain) { this.approverChain = approverChain; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
