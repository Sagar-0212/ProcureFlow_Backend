package com.procureflow.mapper;

import com.procureflow.dto.category.CategoryRequest;
import com.procureflow.dto.category.CategoryResponse;
import com.procureflow.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Maps between Category entity and DTOs.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Category entity, CategoryRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
    }
}
