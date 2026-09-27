package com.procureflow.dto.receipt;

import com.procureflow.enums.GoodsReceiptStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponse {
    private Long id;
    private String receiptNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private LocalDate receivedDate;
    private Long receivedByUserId;
    private String receivedByUserName;
    private String notes;
    private GoodsReceiptStatus status;
    private List<GoodsReceiptItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
