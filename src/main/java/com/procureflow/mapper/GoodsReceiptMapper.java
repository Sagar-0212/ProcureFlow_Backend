package com.procureflow.mapper;

import com.procureflow.dto.receipt.GoodsReceiptItemResponse;
import com.procureflow.dto.receipt.GoodsReceiptResponse;
import com.procureflow.entity.GoodsReceipt;
import com.procureflow.entity.GoodsReceiptItem;
import com.procureflow.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoodsReceiptMapper {

    public GoodsReceiptResponse toResponse(GoodsReceipt receipt) {
        if (receipt == null) {
            return null;
        }

        List<GoodsReceiptItemResponse> itemResponses = receipt.getItems() != null ?
                receipt.getItems().stream().map(this::toItemResponse).toList() : List.of();

        User receivedBy = receipt.getReceivedBy();
        String receivedByName = receivedBy != null ?
                (receivedBy.getFirstName() + " " + receivedBy.getLastName()).trim() : null;

        return GoodsReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .purchaseOrderId(receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getId() : null)
                .purchaseOrderNumber(receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getPoNumber() : null)
                .receivedDate(receipt.getReceivedDate())
                .receivedByUserId(receivedBy != null ? receivedBy.getId() : null)
                .receivedByUserName(receivedByName)
                .notes(receipt.getNotes())
                .status(receipt.getStatus())
                .items(itemResponses)
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .build();
    }

    public GoodsReceiptItemResponse toItemResponse(GoodsReceiptItem item) {
        if (item == null) {
            return null;
        }

        return GoodsReceiptItemResponse.builder()
                .id(item.getId())
                .purchaseOrderItemId(item.getPurchaseOrderItem() != null ? item.getPurchaseOrderItem().getId() : null)
                .productId(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getId() : null)
                .productName(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getName() : null)
                .productSku(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getSku() : null)
                .orderedQuantity(item.getPurchaseOrderItem() != null ? item.getPurchaseOrderItem().getQuantity() : null)
                .receivedQuantity(item.getReceivedQuantity())
                .acceptedQuantity(item.getAcceptedQuantity())
                .rejectedQuantity(item.getRejectedQuantity())
                .remarks(item.getRemarks())
                .build();
    }
}
