package com.rajani.makerchecker.config;

import com.rajani.makerchecker.domain.ApprovalPolicy;
import com.rajani.makerchecker.domain.Employee;
import com.rajani.makerchecker.domain.Enums.*;
import com.rajani.makerchecker.repo.ApprovalPolicyRepository;
import com.rajani.makerchecker.repo.EmployeeRepository;
import com.rajani.makerchecker.service.ApprovalService;
import com.rajani.makerchecker.web.Dtos.SubmitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 20-person org, four approval policies, and a handful of live requests
 * so the demo has something to look at on first load.
 */
@Configuration
@ConditionalOnProperty(name = "makerchecker.seed", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Bean
    ApplicationRunner seed(EmployeeRepository employees, ApprovalPolicyRepository policies,
                           ApprovalService approvals) {
        return args -> {
            if (employees.count() > 0) return;

            // ---- org chart: 1 CRO -> 2 VPs -> 4 Directors -> 5 Managers -> 8 Makers ----
            person(employees, "emp-01", "Aarav Mehta",      "Chief Risk Officer",      "Risk",        null,     "ADMIN,APPROVER_L3", 100_000_000);
            person(employees, "emp-02", "Priya Nair",       "VP, Payments",            "Payments",    "emp-01", "APPROVER_L3",        25_000_000);
            person(employees, "emp-03", "Rohit Sharma",     "VP, Compliance",          "Compliance",  "emp-01", "APPROVER_L3",        25_000_000);
            person(employees, "emp-04", "Ananya Rao",       "Director, Payment Ops",   "Payments",    "emp-02", "APPROVER_L2",         5_000_000);
            person(employees, "emp-05", "Vikram Desai",     "Director, Treasury",      "Treasury",    "emp-02", "APPROVER_L2",         5_000_000);
            person(employees, "emp-06", "Sneha Iyer",       "Director, AML",           "Compliance",  "emp-03", "APPROVER_L2",         5_000_000);
            person(employees, "emp-07", "Karthik Menon",    "Director, KYC",           "Compliance",  "emp-03", "APPROVER_L2",         5_000_000);
            person(employees, "emp-08", "Neha Gupta",       "Manager, Settlements",    "Payments",    "emp-04", "APPROVER_L1",         1_000_000);
            person(employees, "emp-09", "Arjun Pillai",     "Manager, Limits",         "Payments",    "emp-04", "APPROVER_L1",         1_000_000);
            person(employees, "emp-10", "Divya Krishnan",   "Manager, Liquidity",      "Treasury",    "emp-05", "APPROVER_L1",         1_000_000);
            person(employees, "emp-11", "Sameer Joshi",     "Manager, Alert Review",   "Compliance",  "emp-06", "APPROVER_L1",         1_000_000);
            person(employees, "emp-12", "Ritu Bansal",      "Manager, Onboarding",     "Compliance",  "emp-07", "APPROVER_L1",         1_000_000);
            person(employees, "emp-13", "Aditya Verma",     "Settlement Analyst",      "Payments",    "emp-08", "MAKER",                       0);
            person(employees, "emp-14", "Kavya Reddy",      "Settlement Analyst",      "Payments",    "emp-08", "MAKER",                       0);
            person(employees, "emp-15", "Manish Tiwari",    "Limits Analyst",          "Payments",    "emp-09", "MAKER",                       0);
            person(employees, "emp-16", "Pooja Shetty",     "Treasury Analyst",        "Treasury",    "emp-10", "MAKER",                       0);
            person(employees, "emp-17", "Rahul Bose",       "AML Analyst",             "Compliance",  "emp-11", "MAKER",                       0);
            person(employees, "emp-18", "Ishita Chawla",    "AML Analyst",             "Compliance",  "emp-11", "MAKER",                       0);
            person(employees, "emp-19", "Nikhil Kulkarni",  "KYC Officer",             "Compliance",  "emp-12", "MAKER",                       0);
            person(employees, "emp-20", "Tara Fernandes",   "KYC Officer",             "Compliance",  "emp-12", "MAKER",                       0);

            // ---- policies ----
            policy(policies, "PAYMENT_LIMIT_CHANGE", "Change a customer's transaction limit",
                    2, ApprovalMode.SEQUENTIAL, "APPROVER_L1,APPROVER_L2,APPROVER_L3",
                    true, null, 2880, TimeoutAction.ESCALATE);

            policy(policies, "KYC_RISK_RATING", "Change a customer's KYC risk rating",
                    1, ApprovalMode.SEQUENTIAL, "APPROVER_L2,APPROVER_L3",
                    true, null, 1440, TimeoutAction.EXPIRE);

            policy(policies, "AML_CASE_CLOSURE", "Close or escalate an AML alert",
                    2, ApprovalMode.PARALLEL, "APPROVER_L1,APPROVER_L2",
                    false, null, 720, TimeoutAction.AUTO_REJECT);

            policy(policies, "HIGH_VALUE_TRANSFER", "Release a high-value outbound transfer",
                    2, ApprovalMode.SEQUENTIAL, "APPROVER_L2,APPROVER_L3",
                    true, new BigDecimal("5000000"), 240, TimeoutAction.ESCALATE);

            // Two non-financial policies — the payment/KYC/AML ones above are just sample
            // content. The service itself never looks inside before/after, so any domain
            // that can name an entityType and describe a before/after state fits the same
            // engine: vendor onboarding, access grants, HR changes, whatever a calling
            // system needs signed off.
            policy(policies, "VENDOR_ONBOARDING", "Approve a new vendor before onboarding",
                    2, ApprovalMode.SEQUENTIAL, "APPROVER_L1,APPROVER_L2,APPROVER_L3",
                    true, null, 1440, TimeoutAction.ESCALATE);

            policy(policies, "ACCESS_GRANT", "Grant elevated system access to an employee",
                    1, ApprovalMode.SEQUENTIAL, "APPROVER_L2,APPROVER_L3",
                    true, null, 480, TimeoutAction.EXPIRE);

            // ---- a few live requests ----
            approvals.submit(new SubmitRequest("REQ-1001", "default", "CUSTOMER_LIMIT", "CUST-88213",
                    Operation.UPDATE, "Raise daily transfer limit for Sundar Traders",
                    map("dailyLimit", 200000, "currency", "INR"),
                    map("dailyLimit", 750000, "currency", "INR"),
                    new BigDecimal("750000"), "PAYMENT_LIMIT_CHANGE", null), "emp-15");

            approvals.submit(new SubmitRequest("REQ-1002", "default", "KYC_PROFILE", "CUST-44190",
                    Operation.UPDATE, "Downgrade risk rating after periodic review",
                    map("riskRating", "HIGH", "reviewCycle", "ANNUAL"),
                    map("riskRating", "MEDIUM", "reviewCycle", "BIENNIAL"),
                    null, "KYC_RISK_RATING", null), "emp-19");

            approvals.submit(new SubmitRequest("REQ-1003", "default", "AML_CASE", "CASE-2026-0417",
                    Operation.UPDATE, "Close alert as false positive — structuring pattern",
                    map("status", "UNDER_REVIEW", "disposition", null),
                    map("status", "CLOSED", "disposition", "FALSE_POSITIVE"),
                    null, "AML_CASE_CLOSURE", null), "emp-17");

            approvals.submit(new SubmitRequest("REQ-1004", "default", "OUTBOUND_TRANSFER", "TXN-9930112",
                    Operation.CREATE, "Release vendor settlement to Meridian Logistics",
                    null,
                    map("beneficiary", "Meridian Logistics", "amount", 8400000, "currency", "INR"),
                    new BigDecimal("8400000"), "HIGH_VALUE_TRANSFER", null), "emp-16");

            approvals.submit(new SubmitRequest("REQ-1005", "default", "CUSTOMER_LIMIT", "CUST-10077",
                    Operation.UPDATE, "Reduce limit after adverse media hit",
                    map("dailyLimit", 900000, "currency", "INR"),
                    map("dailyLimit", 100000, "currency", "INR"),
                    new BigDecimal("900000"), "PAYMENT_LIMIT_CHANGE", null), "emp-13");

            approvals.submit(new SubmitRequest("REQ-1006", "default", "AML_CASE", "CASE-2026-0418",
                    Operation.UPDATE, "Escalate alert to investigation — sanctions nexus",
                    map("status", "UNDER_REVIEW"),
                    map("status", "ESCALATED", "disposition", "SAR_CANDIDATE"),
                    null, "AML_CASE_CLOSURE", null), "emp-18");

            approvals.submit(new SubmitRequest("REQ-1007", "default", "VENDOR", "VEND-2201",
                    Operation.CREATE, "Onboard new logistics vendor Meridian Freight",
                    null,
                    map("status", "APPROVED_VENDOR", "riskTier", "LOW", "category", "LOGISTICS"),
                    null, "VENDOR_ONBOARDING", null), "emp-14");

            approvals.submit(new SubmitRequest("REQ-1008", "default", "SYSTEM_ACCESS", "ACCESS-3390",
                    Operation.UPDATE, "Grant production database read access to new hire",
                    map("access", "NONE"),
                    map("access", "READ_ONLY", "system", "PAYMENTS_DB"),
                    null, "ACCESS_GRANT", null), "emp-20");

            log.info("Seeded {} people, {} policies, {} requests",
                    employees.count(), policies.count(), 8);
        };
    }

    private static void person(EmployeeRepository repo, String id, String name, String title,
                               String dept, String managerId, String roles, long limit) {
        Employee e = new Employee();
        e.setId(id);
        e.setName(name);
        e.setTitle(title);
        e.setDepartment(dept);
        e.setEmail(id + "@demobank.example");
        e.setManagerId(managerId);
        e.setRoles(roles);
        e.setApprovalLimit(BigDecimal.valueOf(limit));
        // Demo-only: every seeded account logs in with this same password. See CLAUDE.md Identity section.
        e.setPasswordHash("test123");
        repo.save(e);
    }

    private static void policy(ApprovalPolicyRepository repo, String key, String desc, int min,
                               ApprovalMode mode, String roles, boolean managerChain,
                               BigDecimal threshold, int slaMinutes, TimeoutAction onTimeout) {
        ApprovalPolicy p = new ApprovalPolicy();
        p.setPolicyKey(key);
        p.setDescription(desc);
        p.setMinApprovals(min);
        p.setMode(mode);
        p.setAllowedApproverRoles(roles);
        p.setRequireManagerChain(managerChain);
        p.setAmountThreshold(threshold);
        p.setSlaMinutes(slaMinutes);
        p.setOnTimeout(onTimeout);
        repo.save(p);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
