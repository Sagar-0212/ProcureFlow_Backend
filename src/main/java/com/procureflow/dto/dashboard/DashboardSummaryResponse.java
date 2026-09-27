package com.procureflow.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalPurchaseRequests;
    private long pendingApprovals;
    private long approvedRequests;

    private long totalPurchaseOrders;
    private long pendingPOCount;
    private long partialPOCount;
    private long fullPOCount;

    private BigDecimal totalInvoiceAmount;
    private long pendingInvoices;
    private long pendingInvoicesCount;
    private long mismatchedInvoices;
    private long mismatchedInvoicesCount;
    private BigDecimal paidInvoiceAmount;

    private long totalInventoryItems;
    private long totalInventoryItemsCount;
    private List<LowStockProductDto> lowStockProducts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockProductDto {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer currentStock;
        private Integer minimumStockLevel;
    }
}
