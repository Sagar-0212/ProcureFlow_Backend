package com.procureflow.mapper;

import com.procureflow.dto.purchaserequest.PurchaseRequestItemResponse;
import com.procureflow.dto.purchaserequest.PurchaseRequestResponse;
import com.procureflow.entity.PurchaseRequest;
import com.procureflow.entity.PurchaseRequestItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PurchaseRequestMapper {

    public PurchaseRequestResponse toResponse(PurchaseRequest entity) {
        if (entity == null) {
            return null;
        }

        List<PurchaseRequestItemResponse> itemResponses = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemResponse).toList()
                : Collections.emptyList();

        String fullName = entity.getRequestedBy() != null
                ? (entity.getRequestedBy().getFirstName() + " " + entity.getRequestedBy().getLastName()).trim()
                : null;

        return PurchaseRequestResponse.builder()
                .id(entity.getId())
                .requestNumber(entity.getRequestNumber())
                .requestedById(entity.getRequestedBy() != null ? entity.getRequestedBy().getId() : null)
                .requestedByName(fullName)
                .requestedByEmail(entity.getRequestedBy() != null ? entity.getRequestedBy().getEmail() : null)
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .title(entity.getTitle())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .items(itemResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PurchaseRequestItemResponse toItemResponse(PurchaseRequestItem item) {
        if (item == null) {
            return null;
        }

        return PurchaseRequestItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .unit(item.getProduct() != null ? item.getProduct().getUnit() : null)
                .quantity(item.getQuantity())
                .estimatedUnitPrice(item.getEstimatedUnitPrice())
                .estimatedTotal(item.getEstimatedTotal())
                .build();
    }
}
