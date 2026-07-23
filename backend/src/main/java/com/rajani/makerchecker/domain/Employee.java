package com.rajani.makerchecker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Entity
public class Employee {

    @Id
    private String id;

    private String name;
    private String title;
    private String department;
    private String email;
    private String managerId;

    /** Comma-separated role codes, e.g. "APPROVER_L1,MAKER". */
    private String roles;

    private BigDecimal approvalLimit;

    /** Demo-only credential. Every seeded account uses the password "test123". */
    private String passwordHash;

    public List<String> roleList() {
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.stream(roles.split(",")).map(String::trim).toList();
    }

    public boolean hasAnyRole(List<String> allowed) {
        if (allowed == null || allowed.isEmpty()) return true;
        return roleList().stream().anyMatch(allowed::contains);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public BigDecimal getApprovalLimit() { return approvalLimit; }
    public void setApprovalLimit(BigDecimal approvalLimit) { this.approvalLimit = approvalLimit; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
