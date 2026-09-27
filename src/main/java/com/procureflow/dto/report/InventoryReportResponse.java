package com.procureflow.dto.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportResponse {

    private long totalItems;
    private long totalProductsTracked;
    private long totalStockUnits;
    private long lowStockCount;
    private List<ProductInventorySummary> items;
    private List<ProductInventorySummary> productSummaries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInventorySummary {
        private Long productId;
        private String productSku;
        private String productName;
        private String categoryName;
        private Integer currentStock;
        private Integer minimumStockLevel;
        private boolean isLowStock;
    }
}
