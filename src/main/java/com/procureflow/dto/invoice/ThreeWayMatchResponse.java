package com.procureflow.dto.invoice;

import com.procureflow.enums.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreeWayMatchResponse {
    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private InvoiceStatus matchStatus; // MATCHED or MISMATCH
    private String mismatchReason;
    private boolean matched;
    private List<ItemMatchResult> itemResults;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemMatchResult {
        private Long invoiceItemId;
        private Long purchaseOrderItemId;
        private String productName;
        private Integer poOrderedQuantity;
        private Integer acceptedGoodsReceiptQuantity;
        private Integer invoicedQuantity;
        private BigDecimal poUnitPrice;
        private BigDecimal invoicedUnitPrice;
        private boolean quantityMatched;
        private boolean priceMatched;
        private String mismatchDetails;
    }
}
