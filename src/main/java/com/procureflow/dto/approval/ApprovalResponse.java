package com.procureflow.dto.approval;

import com.procureflow.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {

    private Long id;
    private Long purchaseRequestId;
    private String purchaseRequestNumber;
    private String purchaseRequestTitle;
    private BigDecimal totalAmount;
    private String requestedByEmail;
    private String requestedByName;
    private Long approverId;
    private String approverName;
    private String approverEmail;
    private ApprovalStatus status;
    private String comments;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
