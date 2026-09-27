package com.procureflow.repository;

import com.procureflow.entity.PurchaseOrder;
import com.procureflow.enums.POStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    boolean existsByPoNumber(String poNumber);
    boolean existsByQuotationId(Long quotationId);
    boolean existsByQuotationIdAndStatusNot(Long quotationId, POStatus status);
    List<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
}
