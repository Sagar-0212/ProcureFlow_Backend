package com.procureflow.repository;

import com.procureflow.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductIdOrderByTransactionDateDesc(Long productId);
    boolean existsByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
