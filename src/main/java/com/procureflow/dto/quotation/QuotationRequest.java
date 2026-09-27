package com.procureflow.dto.quotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRequest {

    @NotNull(message = "Purchase Request ID is required")
    private Long purchaseRequestId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Quotation date is required")
    private LocalDate quotationDate;

    private LocalDate validUntil;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    private Integer deliveryDays;

    @NotEmpty(message = "Quotation must contain at least one item")
    @Valid
    private List<QuotationItemRequest> items;
}
