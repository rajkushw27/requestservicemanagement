package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, String> {
    List<ApprovalDecision> findByApprovalRequestIdOrderByCreatedAtAsc(String approvalRequestId);
}
