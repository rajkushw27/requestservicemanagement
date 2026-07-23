package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.ApprovalRequest;
import com.rajani.makerchecker.domain.Enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {
    Optional<ApprovalRequest> findByTenantIdAndRequestId(String tenantId, String requestId);
    List<ApprovalRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, RequestStatus status);
    List<ApprovalRequest> findByMakerIdOrderByCreatedAtDesc(String makerId);
    List<ApprovalRequest> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ApprovalRequest> findByStatusAndDeadlineAtBefore(RequestStatus status, Instant cutoff);
}
