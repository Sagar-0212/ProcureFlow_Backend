package com.procureflow.dto.inventory;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentRequest {

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    private String notes;
}
