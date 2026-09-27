package com.procureflow.repository;

import com.procureflow.entity.PurchaseRequest;
import com.procureflow.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    Optional<PurchaseRequest> findByRequestNumber(String requestNumber);
    boolean existsByRequestNumber(String requestNumber);
    List<PurchaseRequest> findByRequestedById(Long userId);
    List<PurchaseRequest> findByStatusIn(List<RequestStatus> statuses);
}
