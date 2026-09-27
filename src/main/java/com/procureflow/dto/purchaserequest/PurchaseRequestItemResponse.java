package com.procureflow.dto.purchaserequest;

import com.procureflow.enums.ProductUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestItemResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private ProductUnit unit;
    private Integer quantity;
    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedTotal;
}
