package com.procureflow.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseReportResponse {
    private long totalRequests;
    private long totalPRCount;
    private BigDecimal totalPROrderedAmount;
    private Map<String, Long> requestsByStatus;

    private long totalPOCount;
    private BigDecimal totalPOAmount;
    private Map<String, Long> ordersByStatus;
}
