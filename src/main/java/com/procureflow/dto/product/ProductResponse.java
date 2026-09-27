package com.procureflow.dto.product;

import com.procureflow.enums.ProductUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private ProductUnit unit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
