package com.procureflow.mapper;

import com.procureflow.dto.po.PurchaseOrderItemResponse;
import com.procureflow.dto.po.PurchaseOrderResponse;
import com.procureflow.entity.PurchaseOrder;
import com.procureflow.entity.PurchaseOrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder po) {
        if (po == null) {
            return null;
        }

        List<PurchaseOrderItemResponse> itemResponses = po.getItems() != null ?
                po.getItems().stream().map(this::toItemResponse).toList() : List.of();

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .purchaseRequestId(po.getPurchaseRequest() != null ? po.getPurchaseRequest().getId() : null)
                .purchaseRequestNumber(po.getPurchaseRequest() != null ? po.getPurchaseRequest().getRequestNumber() : null)
                .quotationId(po.getQuotation() != null ? po.getQuotation().getId() : null)
                .quotationNumber(po.getQuotation() != null ? po.getQuotation().getQuotationNumber() : null)
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getCompanyName() : null)
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .paymentTerms(po.getPaymentTerms())
                .status(po.getStatus())
                .subtotal(po.getSubtotal())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .items(itemResponses)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        if (item == null) {
            return null;
        }

        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
