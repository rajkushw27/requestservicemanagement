package com.rajani.makerchecker.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rajani.makerchecker.domain.ApprovalDecision;
import com.rajani.makerchecker.domain.ApprovalRequest;
import com.rajani.makerchecker.domain.AuditEvent;
import com.rajani.makerchecker.service.ApprovalService;
import com.rajani.makerchecker.service.PolicyEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/approval-requests")
public class ApprovalRequestController {

    private final ApprovalService approvals;
    private final PolicyEngine policyEngine;
    private final AuthSupport auth;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public ApprovalRequestController(ApprovalService approvals, PolicyEngine policyEngine, AuthSupport auth) {
        this.approvals = approvals;
        this.policyEngine = policyEngine;
        this.auth = auth;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                                        @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                                        @RequestBody Dtos.SubmitRequest body) {
        String makerId = auth.currentUser(authorization, apiKey, actingAs);
        ApprovalRequest req = approvals.submit(body, makerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(detail(req));
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                           @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                           @RequestParam(required = false) String inboxFor,
                                           @RequestParam(required = false) String submittedBy) {
        auth.currentUser(authorization, apiKey, actingAs);
        List<ApprovalRequest> results;
        if (inboxFor != null) results = approvals.inboxFor(inboxFor);
        else if (submittedBy != null) results = approvals.submittedBy(submittedBy);
        else results = approvals.all();
        return results.stream().map(this::summary).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                    @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                    @PathVariable String id) {
        auth.currentUser(authorization, apiKey, actingAs);
        return detail(approvals.load(id));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEvent> audit(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                   @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                   @PathVariable String id) {
        auth.currentUser(authorization, apiKey, actingAs);
        return approvals.auditFor(id);
    }

    @PostMapping("/{id}/decisions")
    public Map<String, Object> decide(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                       @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                       @PathVariable String id, @RequestBody Dtos.DecideRequest body) {
        String checkerId = auth.currentUser(authorization, apiKey, actingAs);
        ApprovalRequest req = approvals.decide(id, checkerId, body);
        return detail(req);
    }

    @PostMapping("/{id}/recall")
    public Map<String, Object> recall(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                       @RequestHeader(value = "X-Acting-As", required = false) String actingAs,
                                       @PathVariable String id) {
        String makerId = auth.currentUser(authorization, apiKey, actingAs);
        ApprovalRequest req = approvals.recall(id, makerId);
        return detail(req);
    }

    // ---------- view assembly ----------

    private Map<String, Object> summary(ApprovalRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", req.getId());
        m.put("requestId", req.getRequestId());
        m.put("entityType", req.getEntityType());
        m.put("entityId", req.getEntityId());
        m.put("operation", req.getOperation());
        m.put("summary", req.getSummary());
        m.put("amount", req.getAmount());
        m.put("makerId", req.getMakerId());
        m.put("policyKey", req.getPolicyKey());
        m.put("status", req.getStatus());
        m.put("requiredApprovals", req.getRequiredApprovals());
        m.put("mode", req.getMode());
        m.put("deadlineAt", req.getDeadlineAt());
        m.put("createdAt", req.getCreatedAt());
        m.put("version", req.getVersion());
        return m;
    }

    private Map<String, Object> detail(ApprovalRequest req) {
        Map<String, Object> m = summary(req);
        m.put("before", readJson(req.getPayloadBefore()));
        m.put("after", readJson(req.getPayloadAfter()));
        m.put("decidedAt", req.getDecidedAt());
        m.put("callbackUrl", req.getCallbackUrl());

        List<ApprovalDecision> decisions = approvals.decisionsFor(req.getId());
        List<String> decidedBy = decisions.stream().map(ApprovalDecision::getCheckerId).toList();
        List<String> chain = policyEngine.chainOf(req);

        List<Map<String, Object>> chainView = chain.stream().map(approverId -> {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("employeeId", approverId);
            ApprovalDecision d = decisions.stream().filter(x -> x.getCheckerId().equals(approverId)).findFirst().orElse(null);
            c.put("decision", d == null ? null : d.getDecision());
            c.put("comments", d == null ? null : d.getComments());
            c.put("decidedAt", d == null ? null : d.getCreatedAt());
            return c;
        }).toList();
        m.put("approverChain", chainView);
        m.put("currentlyActionableBy", policyEngine.currentlyActionableBy(req, decidedBy));
        m.put("decisions", decisions);

        return m;
    }

    private Object readJson(String value) {
        if (value == null) return null;
        try { return json.readValue(value, Object.class); }
        catch (Exception e) { return value; }
    }
}
