package com.procureflow.mapper;

import com.procureflow.dto.approval.ApprovalRuleRequest;
import com.procureflow.dto.approval.ApprovalRuleResponse;
import com.procureflow.entity.ApprovalRule;
import org.springframework.stereotype.Component;

@Component
public class ApprovalRuleMapper {

    public ApprovalRuleResponse toResponse(ApprovalRule entity) {
        if (entity == null) {
            return null;
        }

        return ApprovalRuleResponse.builder()
                .id(entity.getId())
                .minimumAmount(entity.getMinimumAmount())
                .maximumAmount(entity.getMaximumAmount())
                .requiredRole(entity.getRequiredRole())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ApprovalRule toEntity(ApprovalRuleRequest request) {
        return ApprovalRule.builder()
                .minimumAmount(request.getMinimumAmount())
                .maximumAmount(request.getMaximumAmount())
                .requiredRole(request.getRequiredRole())
                .active(true)
                .build();
    }

    public void updateEntity(ApprovalRule entity, ApprovalRuleRequest request) {
        entity.setMinimumAmount(request.getMinimumAmount());
        entity.setMaximumAmount(request.getMaximumAmount());
        entity.setRequiredRole(request.getRequiredRole());
    }
}
