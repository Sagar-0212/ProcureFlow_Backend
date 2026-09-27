package com.procureflow.entity;

import com.procureflow.enums.ProductUnit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private ProductUnit unit;

    @Column(name = "minimum_stock_level", columnDefinition = "integer default 10")
    @Builder.Default
    private Integer minimumStockLevel = 10;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
