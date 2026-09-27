package com.procureflow.mapper;

import com.procureflow.dto.inventory.InventoryResponse;
import com.procureflow.dto.inventory.InventoryTransactionResponse;
import com.procureflow.entity.Inventory;
import com.procureflow.entity.InventoryTransaction;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .productUnit(inventory.getProduct() != null && inventory.getProduct().getUnit() != null ?
                        inventory.getProduct().getUnit().name() : null)
                .quantity(inventory.getQuantity())
                .lastUpdated(inventory.getLastUpdated())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public InventoryTransactionResponse toTransactionResponse(InventoryTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .productId(transaction.getProduct() != null ? transaction.getProduct().getId() : null)
                .productName(transaction.getProduct() != null ? transaction.getProduct().getName() : null)
                .productSku(transaction.getProduct() != null ? transaction.getProduct().getSku() : null)
                .transactionType(transaction.getTransactionType())
                .quantity(transaction.getQuantity())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .balanceAfter(transaction.getBalanceAfter())
                .notes(transaction.getNotes())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
