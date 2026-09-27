package com.procureflow.dto.po;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseOrderRequest {

    @NotNull(message = "Quotation ID is required")
    private Long quotationId;

    private LocalDate expectedDeliveryDate;

    private String paymentTerms;

    private String notes;

    private List<PurchaseOrderItemRequest> items;
}
