package com.procureflow.dto.inventory;

import com.procureflow.enums.InventoryTransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private InventoryTransactionType transactionType;
    private Integer quantity;
    private String referenceType;
    private Long referenceId;
    private Integer balanceAfter;
    private String notes;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
}
