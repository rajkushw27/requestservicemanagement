package com.rajani.makerchecker.service;

import com.rajani.makerchecker.domain.ApprovalPolicy;
import com.rajani.makerchecker.domain.ApprovalRequest;
import com.rajani.makerchecker.domain.Employee;
import com.rajani.makerchecker.repo.ApprovalPolicyRepository;
import com.rajani.makerchecker.repo.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Decides who is allowed to check a request, and in what order.
 * This is the only class that knows about the org hierarchy — swap it out
 * (LDAP, an external entitlements service) without touching the state machine.
 */
@Service
public class PolicyEngine {

    private final EmployeeRepository employees;
    private final ApprovalPolicyRepository policies;

    public PolicyEngine(EmployeeRepository employees, ApprovalPolicyRepository policies) {
        this.employees = employees;
        this.policies = policies;
    }

    public ApprovalPolicy policyFor(String policyKey) {
        return policies.findById(policyKey)
                .orElseThrow(() -> ApprovalException.badRequest("Unknown policyKey: " + policyKey));
    }

    /**
     * Resolve an ordered approver chain at submit time and freeze it onto the request,
     * so a later policy edit cannot retroactively change who was supposed to approve.
     */
    public List<String> resolveApprovers(Employee maker, ApprovalPolicy policy, BigDecimal amount) {
        int needed = policy.requiredApprovals(amount);
        List<String> chain = new ArrayList<>();

        if (policy.isRequireManagerChain()) {
            String cursor = maker.getManagerId();
            Set<String> seen = new HashSet<>();
            while (cursor != null && seen.add(cursor) && chain.size() < needed) {
                Employee candidate = employees.findById(cursor).orElse(null);
                if (candidate == null) break;
                if (isEligible(candidate, maker, policy, amount)) chain.add(candidate.getId());
                cursor = candidate.getManagerId();
            }
        }

        // Backfill from the wider org if the management chain could not satisfy the policy.
        if (chain.size() < needed) {
            employees.findAllByOrderByIdAsc().stream()
                    .filter(e -> !chain.contains(e.getId()))
                    .filter(e -> isEligible(e, maker, policy, amount))
                    .sorted(Comparator.comparing(Employee::getApprovalLimit))
                    .limit(needed - chain.size())
                    .forEach(e -> chain.add(e.getId()));
        }

        if (chain.size() < needed) {
            throw ApprovalException.badRequest(
                "Policy " + policy.getPolicyKey() + " needs " + needed +
                " approver(s) but only " + chain.size() + " eligible people exist for this maker.");
        }
        return chain;
    }

    private boolean isEligible(Employee candidate, Employee maker, ApprovalPolicy policy, BigDecimal amount) {
        // Segregation of duties: the maker can never be their own checker.
        if (policy.isExcludeMaker() && candidate.getId().equals(maker.getId())) return false;
        if (!candidate.hasAnyRole(policy.allowedRoleList())) return false;
        if (policy.isEnforceApprovalLimit() && amount != null
                && candidate.getApprovalLimit().compareTo(amount) < 0) return false;
        return true;
    }

    /** Throws unless this checker may act on this request right now. */
    public void assertCanCheck(ApprovalRequest req, String checkerId, List<String> alreadyDecidedBy) {
        List<String> chain = chainOf(req);

        if (checkerId.equals(req.getMakerId())) {
            throw ApprovalException.forbidden("Segregation of duties: the maker cannot approve their own request.");
        }
        if (!chain.contains(checkerId)) {
            throw ApprovalException.forbidden("You are not an assigned approver for this request.");
        }
        if (alreadyDecidedBy.contains(checkerId)) {
            throw ApprovalException.conflict("You have already recorded a decision on this request.");
        }
        if (req.getMode() == com.rajani.makerchecker.domain.Enums.ApprovalMode.SEQUENTIAL) {
            String expected = nextApprover(req, alreadyDecidedBy);
            if (!checkerId.equals(expected)) {
                throw ApprovalException.forbidden("Sequential policy: it is not your turn yet.");
            }
        }
    }

    /** Who the request is waiting on. Null once every required approval is in. */
    public String nextApprover(ApprovalRequest req, List<String> alreadyDecidedBy) {
        for (String id : chainOf(req)) {
            if (!alreadyDecidedBy.contains(id)) return id;
        }
        return null;
    }

    /** In PARALLEL mode everyone still outstanding can act; in SEQUENTIAL only the next one. */
    public List<String> currentlyActionableBy(ApprovalRequest req, List<String> alreadyDecidedBy) {
        if (req.getMode() == com.rajani.makerchecker.domain.Enums.ApprovalMode.PARALLEL) {
            return chainOf(req).stream().filter(id -> !alreadyDecidedBy.contains(id)).toList();
        }
        String next = nextApprover(req, alreadyDecidedBy);
        return next == null ? List.of() : List.of(next);
    }

    public List<String> chainOf(ApprovalRequest req) {
        if (req.getApproverChain() == null || req.getApproverChain().isBlank()) return List.of();
        return Arrays.stream(req.getApproverChain().split(",")).map(String::trim).toList();
    }
}
