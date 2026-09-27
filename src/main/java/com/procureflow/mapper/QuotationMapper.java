package com.procureflow.mapper;

import com.procureflow.dto.quotation.QuotationItemResponse;
import com.procureflow.dto.quotation.QuotationResponse;
import com.procureflow.entity.Quotation;
import com.procureflow.entity.QuotationItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class QuotationMapper {

    public QuotationResponse toResponse(Quotation entity) {
        if (entity == null) {
            return null;
        }

        List<QuotationItemResponse> itemResponses = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemResponse).toList()
                : Collections.emptyList();

        return QuotationResponse.builder()
                .id(entity.getId())
                .quotationNumber(entity.getQuotationNumber())
                .purchaseRequestId(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getId() : null)
                .purchaseRequestNumber(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getRequestNumber() : null)
                .purchaseRequestTitle(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getTitle() : null)
                .supplierId(entity.getSupplier() != null ? entity.getSupplier().getId() : null)
                .supplierCode(entity.getSupplier() != null ? entity.getSupplier().getSupplierCode() : null)
                .supplierCompanyName(entity.getSupplier() != null ? entity.getSupplier().getCompanyName() : null)
                .quotationDate(entity.getQuotationDate())
                .validUntil(entity.getValidUntil())
                .paymentTerms(entity.getPaymentTerms())
                .deliveryDays(entity.getDeliveryDays())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .items(itemResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public QuotationItemResponse toItemResponse(QuotationItem item) {
        if (item == null) {
            return null;
        }

        return QuotationItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .unit(item.getProduct() != null ? item.getProduct().getUnit() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
