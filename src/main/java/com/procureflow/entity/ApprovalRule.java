package com.procureflow.entity;

import com.procureflow.enums.RoleName;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "approval_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "minimum_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumAmount;

    @NotNull
    @Column(name = "maximum_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", nullable = false, length = 50)
    private RoleName requiredRole;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
