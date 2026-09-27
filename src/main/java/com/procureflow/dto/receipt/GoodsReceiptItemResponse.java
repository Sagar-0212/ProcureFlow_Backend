package com.procureflow.dto.receipt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItemResponse {
    private Long id;
    private Long purchaseOrderItemId;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;
    private String remarks;
}
