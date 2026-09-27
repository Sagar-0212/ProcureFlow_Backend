package com.procureflow.mapper;

import com.procureflow.dto.product.ProductRequest;
import com.procureflow.dto.product.ProductResponse;
import com.procureflow.entity.Category;
import com.procureflow.entity.Product;
import org.springframework.stereotype.Component;

/**
 * Maps between Product entity and DTOs.
 * Handles the Category relationship by flattening it into categoryId/categoryName.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product entity) {
        return ProductResponse.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .description(entity.getDescription())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .unit(entity.getUnit())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .unit(request.getUnit())
                .build();
    }

    public void updateEntity(Product entity, ProductRequest request, Category category) {
        entity.setSku(request.getSku());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCategory(category);
        entity.setUnit(request.getUnit());
    }
}
