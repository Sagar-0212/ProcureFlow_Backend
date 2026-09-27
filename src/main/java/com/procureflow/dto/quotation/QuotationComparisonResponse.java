package com.procureflow.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationComparisonResponse {

    private Long purchaseRequestId;
    private String purchaseRequestNumber;
    private String purchaseRequestTitle;
    private BigDecimal estimatedTotalAmount;
    private List<QuotationResponse> quotations;
}
