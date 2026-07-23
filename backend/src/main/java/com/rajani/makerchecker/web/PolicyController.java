package com.rajani.makerchecker.web;

import com.rajani.makerchecker.domain.ApprovalPolicy;
import com.rajani.makerchecker.domain.Enums.ApprovalMode;
import com.rajani.makerchecker.domain.Enums.TimeoutAction;
import com.rajani.makerchecker.repo.ApprovalPolicyRepository;
import com.rajani.makerchecker.service.ApprovalException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final ApprovalPolicyRepository policies;
    private final AuthSupport auth;

    public PolicyController(ApprovalPolicyRepository policies, AuthSupport auth) {
        this.policies = policies;
        this.auth = auth;
    }

    @GetMapping
    public List<ApprovalPolicy> list(@RequestHeader("Authorization") String authorization) {
        auth.currentUser(authorization);
        return policies.findAll();
    }

    @PutMapping("/{key}")
    public ApprovalPolicy update(@RequestHeader("Authorization") String authorization,
                                  @PathVariable String key, @RequestBody Dtos.PolicyUpdateRequest body) {
        auth.currentUser(authorization);
        ApprovalPolicy policy = policies.findById(key)
                .orElseThrow(() -> ApprovalException.notFound("Unknown policyKey: " + key));

        if (body.description() != null) policy.setDescription(body.description());
        if (body.minApprovals() != null) policy.setMinApprovals(body.minApprovals());
        if (body.mode() != null) policy.setMode(ApprovalMode.valueOf(body.mode()));
        if (body.allowedApproverRoles() != null) policy.setAllowedApproverRoles(body.allowedApproverRoles());
        if (body.requireManagerChain() != null) policy.setRequireManagerChain(body.requireManagerChain());
        if (body.excludeMaker() != null) policy.setExcludeMaker(body.excludeMaker());
        if (body.enforceApprovalLimit() != null) policy.setEnforceApprovalLimit(body.enforceApprovalLimit());
        if (body.allowSelfRecall() != null) policy.setAllowSelfRecall(body.allowSelfRecall());
        if (body.amountThreshold() != null) policy.setAmountThreshold(body.amountThreshold());
        if (body.slaMinutes() != null) policy.setSlaMinutes(body.slaMinutes());
        if (body.onTimeout() != null) policy.setOnTimeout(TimeoutAction.valueOf(body.onTimeout()));

        return policies.save(policy);
    }
}
