package com.rajani.makerchecker.web;

import com.rajani.makerchecker.domain.Enums.DecisionType;
import com.rajani.makerchecker.domain.Enums.Operation;

import java.math.BigDecimal;
import java.util.Map;

public class Dtos {

    public record SubmitRequest(
            String requestId,
            String tenantId,
            String entityType,
            String entityId,
            Operation operation,
            String summary,
            Map<String, Object> before,
            Map<String, Object> after,
            BigDecimal amount,
            String policyKey,
            String callbackUrl
    ) {}

    public record DecideRequest(
            DecisionType decision,
            String comments,
            Long expectedVersion
    ) {}

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token, EmployeeView employee) {}

    public record EmployeeView(
            String id, String name, String title, String department,
            String managerId, java.util.List<String> roles, BigDecimal approvalLimit
    ) {}

    public record PolicyUpdateRequest(
            String description,
            Integer minApprovals,
            String mode,
            String allowedApproverRoles,
            Boolean requireManagerChain,
            Boolean excludeMaker,
            Boolean enforceApprovalLimit,
            Boolean allowSelfRecall,
            BigDecimal amountThreshold,
            Integer slaMinutes,
            String onTimeout
    ) {}

    public record ApiError(String error, String message) {}

    private Dtos() {}
}
