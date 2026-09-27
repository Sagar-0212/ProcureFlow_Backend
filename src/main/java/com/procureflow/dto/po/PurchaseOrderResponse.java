package com.procureflow.dto.po;

import com.procureflow.enums.POStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long purchaseRequestId;
    private String purchaseRequestNumber;
    private Long quotationId;
    private String quotationNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String paymentTerms;
    private POStatus status;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private String notes;
    private List<PurchaseOrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
