package com.procureflow.dto.quotation;

import com.procureflow.enums.QuotationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponse {

    private Long id;
    private String quotationNumber;
    private Long purchaseRequestId;
    private String purchaseRequestNumber;
    private String purchaseRequestTitle;
    private Long supplierId;
    private String supplierCode;
    private String supplierCompanyName;
    private LocalDate quotationDate;
    private LocalDate validUntil;
    private String paymentTerms;
    private Integer deliveryDays;
    private BigDecimal totalAmount;
    private QuotationStatus status;
    private List<QuotationItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
