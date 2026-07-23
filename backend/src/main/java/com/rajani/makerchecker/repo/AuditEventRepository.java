package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findByApprovalRequestIdOrderByCreatedAtAsc(String approvalRequestId);
}
