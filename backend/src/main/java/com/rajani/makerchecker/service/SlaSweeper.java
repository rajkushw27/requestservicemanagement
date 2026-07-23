package com.rajani.makerchecker.service;

import com.rajani.makerchecker.domain.ApprovalRequest;
import com.rajani.makerchecker.domain.Enums.RequestStatus;
import com.rajani.makerchecker.repo.ApprovalRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Sweeps PENDING requests past their deadline and applies the policy's timeout action. */
@Component
public class SlaSweeper {

    private final ApprovalRequestRepository requests;
    private final ApprovalService approvalService;

    public SlaSweeper(ApprovalRequestRepository requests, ApprovalService approvalService) {
        this.requests = requests;
        this.approvalService = approvalService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        List<ApprovalRequest> overdue = requests.findByStatusAndDeadlineAtBefore(RequestStatus.PENDING, Instant.now());
        for (ApprovalRequest req : overdue) {
            approvalService.applyTimeout(req);
        }
    }
}
