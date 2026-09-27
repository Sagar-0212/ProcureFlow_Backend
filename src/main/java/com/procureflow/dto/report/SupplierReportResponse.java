package com.procureflow.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReportResponse {

    private long totalSuppliers;
    private long totalSuppliersCount;
    private long activeSuppliers;
    private long activeSuppliersCount;
    private List<SupplierPerformanceSummary> supplierSummaries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierPerformanceSummary {
        private Long supplierId;
        private String supplierCode;
        private String companyName;
        private long totalQuotationsSubmitted;
        private long totalQuotationsSelected;
        private long totalPOsIssued;
        private BigDecimal totalPOAmount;
    }
}
