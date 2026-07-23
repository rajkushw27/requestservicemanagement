package com.rajani.makerchecker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rajani.makerchecker.domain.*;
import com.rajani.makerchecker.domain.Enums.*;
import com.rajani.makerchecker.repo.*;
import com.rajani.makerchecker.web.Dtos.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The state machine. DRAFT -> PENDING -> APPROVED | REJECTED | EXPIRED | RECALLED.
 * Every transition writes an audit row and (on terminal states) an outbox event,
 * in the same transaction as the status change.
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository requests;
    private final ApprovalDecisionRepository decisions;
    private final AuditEventRepository audit;
    private final OutboxEventRepository outbox;
    private final EmployeeRepository employees;
    private final PolicyEngine policyEngine;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public ApprovalService(ApprovalRequestRepository requests, ApprovalDecisionRepository decisions,
                           AuditEventRepository audit, OutboxEventRepository outbox,
                           EmployeeRepository employees, PolicyEngine policyEngine) {
        this.requests = requests;
        this.decisions = decisions;
        this.audit = audit;
        this.outbox = outbox;
        this.employees = employees;
        this.policyEngine = policyEngine;
    }

    // ---------- maker ----------

    @Transactional
    public ApprovalRequest submit(SubmitRequest body, String makerId) {
        String tenant = body.tenantId() == null ? "default" : body.tenantId();

        // Idempotency: the same requestId for the same tenant returns the original.
        Optional<ApprovalRequest> existing = requests.findByTenantIdAndRequestId(tenant, body.requestId());
        if (existing.isPresent()) return existing.get();

        Employee maker = employees.findById(makerId)
                .orElseThrow(() -> ApprovalException.badRequest("Unknown maker: " + makerId));

        ApprovalPolicy policy = policyEngine.policyFor(body.policyKey());
        List<String> chain = policyEngine.resolveApprovers(maker, policy, body.amount());

        ApprovalRequest req = new ApprovalRequest();
        req.setRequestId(body.requestId());
        req.setTenantId(tenant);
        req.setEntityType(body.entityType());
        req.setEntityId(body.entityId());
        req.setOperation(body.operation() == null ? Operation.UPDATE : body.operation());
        req.setSummary(body.summary());
        req.setPayloadBefore(writeJson(body.before()));
        req.setPayloadAfter(writeJson(body.after()));
        req.setAmount(body.amount());
        req.setMakerId(makerId);
        req.setPolicyKey(policy.getPolicyKey());
        req.setStatus(RequestStatus.PENDING);
        req.setRequiredApprovals(policy.requiredApprovals(body.amount()));
        req.setMode(policy.getMode());
        req.setApproverChain(String.join(",", chain));
        req.setDeadlineAt(Instant.now().plus(policy.getSlaMinutes(), ChronoUnit.MINUTES));
        req.setCallbackUrl(body.callbackUrl());

        requests.save(req);
        audit.save(AuditEvent.of(req.getId(), tenant, makerId, "SUBMITTED", "DRAFT", "PENDING",
                "policy=" + policy.getPolicyKey() + " approvers=" + chain + " required=" + req.getRequiredApprovals()));
        return req;
    }

    @Transactional
    public ApprovalRequest recall(String id, String makerId) {
        ApprovalRequest req = load(id);
        if (!req.getMakerId().equals(makerId)) {
            throw ApprovalException.forbidden("Only the maker can withdraw this request.");
        }
        if (req.getStatus() != RequestStatus.PENDING) {
            throw ApprovalException.conflict("This request is already " + req.getStatus() + ".");
        }
        ApprovalPolicy policy = policyEngine.policyFor(req.getPolicyKey());
        if (!policy.isAllowSelfRecall()) {
            throw ApprovalException.forbidden("Policy " + policy.getPolicyKey() + " does not allow withdrawal.");
        }
        return finish(req, RequestStatus.RECALLED, makerId, "RECALLED", "Withdrawn by maker");
    }

    // ---------- checker ----------

    @Transactional
    public ApprovalRequest decide(String id, String checkerId, DecideRequest body) {
        ApprovalRequest req = load(id);

        if (req.getStatus() != RequestStatus.PENDING) {
            throw ApprovalException.conflict("This request is already " + req.getStatus() + ".");
        }
        // Optimistic lock check: two checkers racing on the same request cannot both win.
        if (body.expectedVersion() != null && body.expectedVersion() != req.getVersion()) {
            throw ApprovalException.conflict("This request changed while you were reviewing it. Reload and try again.");
        }
        if (body.decision() != DecisionType.APPROVE
                && (body.comments() == null || body.comments().isBlank())) {
            throw ApprovalException.badRequest("A comment is required when you reject or send back a request.");
        }

        List<ApprovalDecision> prior = decisions.findByApprovalRequestIdOrderByCreatedAtAsc(id);
        List<String> decidedBy = prior.stream().map(ApprovalDecision::getCheckerId).toList();

        policyEngine.assertCanCheck(req, checkerId, decidedBy);

        ApprovalDecision d = new ApprovalDecision();
        d.setApprovalRequestId(id);
        d.setCheckerId(checkerId);
        d.setDecision(body.decision());
        d.setComments(body.comments());
        d.setSequenceIndex(prior.size());
        decisions.save(d);

        audit.save(AuditEvent.of(id, req.getTenantId(), checkerId, "DECISION_" + body.decision(),
                "PENDING", "PENDING", body.comments()));

        if (body.decision() == DecisionType.REJECT || body.decision() == DecisionType.REQUEST_CHANGES) {
            return finish(req, RequestStatus.REJECTED, checkerId, "REJECTED", body.comments());
        }

        long approvals = prior.stream().filter(p -> p.getDecision() == DecisionType.APPROVE).count() + 1;
        if (approvals >= req.getRequiredApprovals()) {
            return finish(req, RequestStatus.APPROVED, checkerId, "APPROVED",
                    "All " + req.getRequiredApprovals() + " approval(s) received");
        }

        // Still short of the required count: stays PENDING, now waiting on the next approver.
        // The unique (request, checker) constraint stops anyone signing twice.
        return req;
    }

    // ---------- SLA ----------

    @Transactional
    public void applyTimeout(ApprovalRequest req) {
        ApprovalPolicy policy = policyEngine.policyFor(req.getPolicyKey());
        switch (policy.getOnTimeout()) {
            case AUTO_REJECT ->
                finish(req, RequestStatus.REJECTED, "SYSTEM", "SLA_AUTO_REJECT",
                        "No decision within " + policy.getSlaMinutes() + " minutes");
            case ESCALATE -> {
                // Push the deadline out and add the next person up the chain, if there is one.
                String top = escalateChain(req);
                req.setDeadlineAt(Instant.now().plus(policy.getSlaMinutes(), ChronoUnit.MINUTES));
                requests.save(req);
                audit.save(AuditEvent.of(req.getId(), req.getTenantId(), "SYSTEM", "SLA_ESCALATED",
                        "PENDING", "PENDING", top == null ? "No one left to escalate to" : "Escalated to " + top));
            }
            case EXPIRE ->
                finish(req, RequestStatus.EXPIRED, "SYSTEM", "SLA_EXPIRED",
                        "No decision within " + policy.getSlaMinutes() + " minutes");
        }
    }

    private String escalateChain(ApprovalRequest req) {
        List<String> chain = new ArrayList<>(policyEngine.chainOf(req));
        if (chain.isEmpty()) return null;
        Employee last = employees.findById(chain.get(chain.size() - 1)).orElse(null);
        if (last == null || last.getManagerId() == null) return null;
        if (chain.contains(last.getManagerId())) return null;
        chain.add(last.getManagerId());
        req.setApproverChain(String.join(",", chain));
        return last.getManagerId();
    }

    // ---------- shared ----------

    private ApprovalRequest finish(ApprovalRequest req, RequestStatus to, String actor,
                                   String action, String detail) {
        String from = req.getStatus().name();
        req.setStatus(to);
        req.setDecidedAt(Instant.now());
        requests.save(req);
        audit.save(AuditEvent.of(req.getId(), req.getTenantId(), actor, action, from, to.name(), detail));
        outbox.save(OutboxEvent.of(req.getId(), "approval." + to.name().toLowerCase(),
                writeJson(outcomePayload(req, actor, detail)), req.getCallbackUrl()));
        return req;
    }

    private Map<String, Object> outcomePayload(ApprovalRequest req, String actor, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("approvalRequestId", req.getId());
        m.put("requestId", req.getRequestId());
        m.put("tenantId", req.getTenantId());
        m.put("entityType", req.getEntityType());
        m.put("entityId", req.getEntityId());
        m.put("operation", req.getOperation());
        m.put("status", req.getStatus());
        m.put("decidedBy", actor);
        m.put("decidedAt", req.getDecidedAt());
        m.put("detail", detail);
        return m;
    }

    public ApprovalRequest load(String id) {
        return requests.findById(id).orElseThrow(() -> ApprovalException.notFound("No request with id " + id));
    }

    public List<ApprovalDecision> decisionsFor(String id) {
        return decisions.findByApprovalRequestIdOrderByCreatedAtAsc(id);
    }

    public List<AuditEvent> auditFor(String id) {
        return audit.findByApprovalRequestIdOrderByCreatedAtAsc(id);
    }

    /** Everything this person can act on right now. */
    public List<ApprovalRequest> inboxFor(String employeeId) {
        return requests.findByTenantIdAndStatusOrderByCreatedAtDesc("default", RequestStatus.PENDING).stream()
                .filter(r -> {
                    List<String> decidedBy = decisions.findByApprovalRequestIdOrderByCreatedAtAsc(r.getId())
                            .stream().map(ApprovalDecision::getCheckerId).toList();
                    return policyEngine.currentlyActionableBy(r, decidedBy).contains(employeeId);
                })
                .toList();
    }

    public List<ApprovalRequest> submittedBy(String employeeId) {
        return requests.findByMakerIdOrderByCreatedAtDesc(employeeId);
    }

    public List<ApprovalRequest> all() {
        return requests.findByTenantIdOrderByCreatedAtDesc("default");
    }

    private String writeJson(Object o) {
        if (o == null) return null;
        try { return json.writeValueAsString(o); }
        catch (Exception e) { throw ApprovalException.badRequest("Payload is not valid JSON: " + e.getMessage()); }
    }
}
