package com.procureflow.mapper;

import com.procureflow.dto.approval.ApprovalResponse;
import com.procureflow.entity.Approval;
import org.springframework.stereotype.Component;

@Component
public class ApprovalMapper {

    public ApprovalResponse toResponse(Approval entity) {
        if (entity == null) {
            return null;
        }

        String reqByName = entity.getPurchaseRequest() != null && entity.getPurchaseRequest().getRequestedBy() != null
                ? (entity.getPurchaseRequest().getRequestedBy().getFirstName() + " " + entity.getPurchaseRequest().getRequestedBy().getLastName()).trim()
                : null;

        String approverName = entity.getApprover() != null
                ? (entity.getApprover().getFirstName() + " " + entity.getApprover().getLastName()).trim()
                : null;

        return ApprovalResponse.builder()
                .id(entity.getId())
                .purchaseRequestId(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getId() : null)
                .purchaseRequestNumber(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getRequestNumber() : null)
                .purchaseRequestTitle(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getTitle() : null)
                .totalAmount(entity.getPurchaseRequest() != null ? entity.getPurchaseRequest().getTotalAmount() : null)
                .requestedByEmail(entity.getPurchaseRequest() != null && entity.getPurchaseRequest().getRequestedBy() != null
                        ? entity.getPurchaseRequest().getRequestedBy().getEmail() : null)
                .requestedByName(reqByName)
                .approverId(entity.getApprover() != null ? entity.getApprover().getId() : null)
                .approverName(approverName)
                .approverEmail(entity.getApprover() != null ? entity.getApprover().getEmail() : null)
                .status(entity.getStatus())
                .comments(entity.getComments())
                .approvedAt(entity.getApprovedAt())
                .rejectedAt(entity.getRejectedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
