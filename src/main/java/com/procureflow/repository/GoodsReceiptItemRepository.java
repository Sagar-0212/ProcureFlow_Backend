package com.procureflow.repository;

import com.procureflow.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {

    @Query("SELECT COALESCE(SUM(gri.receivedQuantity), 0) FROM GoodsReceiptItem gri WHERE gri.purchaseOrderItem.id = :poItemId AND gri.goodsReceipt.status = 'RECEIVED'")
    Integer sumReceivedQuantityByPurchaseOrderItemId(@Param("poItemId") Long poItemId);

    @Query("SELECT COALESCE(SUM(gri.acceptedQuantity), 0) FROM GoodsReceiptItem gri WHERE gri.purchaseOrderItem.id = :poItemId AND gri.goodsReceipt.status = 'RECEIVED'")
    Integer sumAcceptedQuantityByPurchaseOrderItemId(@Param("poItemId") Long poItemId);
}
