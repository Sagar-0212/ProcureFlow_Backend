package com.procureflow.repository;

import com.procureflow.entity.Approval;
import com.procureflow.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    Optional<Approval> findByPurchaseRequestId(Long purchaseRequestId);
    List<Approval> findByStatus(ApprovalStatus status);
}
