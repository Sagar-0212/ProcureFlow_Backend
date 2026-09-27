package com.procureflow.mapper;

import com.procureflow.dto.supplier.SupplierRequest;
import com.procureflow.dto.supplier.SupplierResponse;
import com.procureflow.entity.Supplier;
import org.springframework.stereotype.Component;

/**
 * Maps between Supplier entity and DTOs.
 */
@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier entity) {
        return SupplierResponse.builder()
                .id(entity.getId())
                .supplierCode(entity.getSupplierCode())
                .companyName(entity.getCompanyName())
                .contactPerson(entity.getContactPerson())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .taxIdentifier(entity.getTaxIdentifier())
                .paymentTerms(entity.getPaymentTerms())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Supplier toEntity(SupplierRequest request) {
        return Supplier.builder()
                .supplierCode(request.getSupplierCode())
                .companyName(request.getCompanyName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .taxIdentifier(request.getTaxIdentifier())
                .paymentTerms(request.getPaymentTerms())
                .build();
    }

    public void updateEntity(Supplier entity, SupplierRequest request) {
        entity.setSupplierCode(request.getSupplierCode());
        entity.setCompanyName(request.getCompanyName());
        entity.setContactPerson(request.getContactPerson());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setTaxIdentifier(request.getTaxIdentifier());
        entity.setPaymentTerms(request.getPaymentTerms());
    }
}
