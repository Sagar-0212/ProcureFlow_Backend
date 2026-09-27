package com.procureflow.dto.purchaserequest;

import com.procureflow.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestResponse {

    private Long id;
    private String requestNumber;
    private Long requestedById;
    private String requestedByName;
    private String requestedByEmail;
    private Long departmentId;
    private String departmentName;
    private String title;
    private String reason;
    private RequestStatus status;
    private BigDecimal totalAmount;
    private List<PurchaseRequestItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
