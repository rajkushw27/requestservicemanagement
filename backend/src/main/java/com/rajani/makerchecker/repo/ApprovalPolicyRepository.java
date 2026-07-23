package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.ApprovalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, String> {
}
