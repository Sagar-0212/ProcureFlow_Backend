package com.procureflow.dto.receipt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItemRequest {

    @NotNull(message = "Purchase Order Item ID is required")
    private Long purchaseOrderItemId;

    @NotNull(message = "Received quantity is required")
    @Min(value = 1, message = "Received quantity must be greater than 0")
    private Integer receivedQuantity;

    @NotNull(message = "Accepted quantity is required")
    @Min(value = 0, message = "Accepted quantity cannot be negative")
    private Integer acceptedQuantity;

    @NotNull(message = "Rejected quantity is required")
    @Min(value = 0, message = "Rejected quantity cannot be negative")
    private Integer rejectedQuantity;

    private String remarks;
}
