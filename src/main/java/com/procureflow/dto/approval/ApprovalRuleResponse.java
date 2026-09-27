package com.procureflow.dto.approval;

import com.procureflow.enums.RoleName;
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
public class ApprovalRuleResponse {

    private Long id;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private RoleName requiredRole;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
