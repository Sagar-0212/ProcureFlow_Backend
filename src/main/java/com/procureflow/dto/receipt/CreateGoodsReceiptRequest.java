package com.procureflow.dto.receipt;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGoodsReceiptRequest {

    @NotNull(message = "Purchase Order ID is required")
    private Long purchaseOrderId;

    @NotNull(message = "Received date is required")
    private LocalDate receivedDate;

    private String notes;

    @NotEmpty(message = "Goods receipt must contain at least one item")
    private List<GoodsReceiptItemRequest> items;
}
