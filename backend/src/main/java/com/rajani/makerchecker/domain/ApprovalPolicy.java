package com.rajani.makerchecker.domain;

import com.rajani.makerchecker.domain.Enums.ApprovalMode;
import com.rajani.makerchecker.domain.Enums.TimeoutAction;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Entity
public class ApprovalPolicy {

    @Id
    private String policyKey;

    private String description;
    private int minApprovals;

    @Enumerated(EnumType.STRING)
    private ApprovalMode mode;

    /** Comma-separated role codes eligible to approve under this policy. */
    private String allowedApproverRoles;

    private boolean requireManagerChain = true;
    private boolean excludeMaker = true;
    private boolean enforceApprovalLimit = true;
    private boolean allowSelfRecall = true;

    /** Above this amount, one extra signature is required. Null = no bump. */
    private BigDecimal amountThreshold;

    private int slaMinutes;

    @Enumerated(EnumType.STRING)
    private TimeoutAction onTimeout;

    public int requiredApprovals(BigDecimal amount) {
        if (amountThreshold != null && amount != null && amount.compareTo(amountThreshold) > 0) {
            return minApprovals + 1;
        }
        return minApprovals;
    }

    public List<String> allowedRoleList() {
        if (allowedApproverRoles == null || allowedApproverRoles.isBlank()) return List.of();
        return Arrays.stream(allowedApproverRoles.split(",")).map(String::trim).toList();
    }

    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMinApprovals() { return minApprovals; }
    public void setMinApprovals(int minApprovals) { this.minApprovals = minApprovals; }
    public ApprovalMode getMode() { return mode; }
    public void setMode(ApprovalMode mode) { this.mode = mode; }
    public String getAllowedApproverRoles() { return allowedApproverRoles; }
    public void setAllowedApproverRoles(String allowedApproverRoles) { this.allowedApproverRoles = allowedApproverRoles; }
    public boolean isRequireManagerChain() { return requireManagerChain; }
    public void setRequireManagerChain(boolean requireManagerChain) { this.requireManagerChain = requireManagerChain; }
    public boolean isExcludeMaker() { return excludeMaker; }
    public void setExcludeMaker(boolean excludeMaker) { this.excludeMaker = excludeMaker; }
    public boolean isEnforceApprovalLimit() { return enforceApprovalLimit; }
    public void setEnforceApprovalLimit(boolean enforceApprovalLimit) { this.enforceApprovalLimit = enforceApprovalLimit; }
    public boolean isAllowSelfRecall() { return allowSelfRecall; }
    public void setAllowSelfRecall(boolean allowSelfRecall) { this.allowSelfRecall = allowSelfRecall; }
    public BigDecimal getAmountThreshold() { return amountThreshold; }
    public void setAmountThreshold(BigDecimal amountThreshold) { this.amountThreshold = amountThreshold; }
    public int getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(int slaMinutes) { this.slaMinutes = slaMinutes; }
    public TimeoutAction getOnTimeout() { return onTimeout; }
    public void setOnTimeout(TimeoutAction onTimeout) { this.onTimeout = onTimeout; }
}
