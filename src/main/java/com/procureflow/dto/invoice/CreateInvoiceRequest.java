package com.procureflow.dto.invoice;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {

    @NotNull(message = "Purchase Order ID is required")
    private Long purchaseOrderId;

    private Long supplierId;

    private String supplierInvoiceNumber;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private BigDecimal taxAmount;

    private String notes;

    @NotEmpty(message = "Invoice must contain at least one item")
    private List<InvoiceItemRequest> items;
}
