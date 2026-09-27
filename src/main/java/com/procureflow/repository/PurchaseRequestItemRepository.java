package com.procureflow.repository;

import com.procureflow.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {
}
