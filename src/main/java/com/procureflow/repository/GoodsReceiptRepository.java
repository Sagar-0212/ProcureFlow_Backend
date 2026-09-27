package com.procureflow.repository;

import com.procureflow.entity.GoodsReceipt;
import com.procureflow.enums.GoodsReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    boolean existsByReceiptNumber(String receiptNumber);
    List<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);
    List<GoodsReceipt> findByPurchaseOrderIdAndStatus(Long purchaseOrderId, GoodsReceiptStatus status);
    Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);
}
