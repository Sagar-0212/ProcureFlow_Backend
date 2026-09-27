package com.procureflow.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String supplierCode;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String taxIdentifier;
    private String paymentTerms;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
