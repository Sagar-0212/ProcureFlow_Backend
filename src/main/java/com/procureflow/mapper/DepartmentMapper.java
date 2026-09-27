package com.procureflow.mapper;

import com.procureflow.dto.department.DepartmentRequest;
import com.procureflow.dto.department.DepartmentResponse;
import com.procureflow.entity.Department;
import org.springframework.stereotype.Component;

/**
 * Maps between Department entity and DTOs.
 * Keeps conversion logic separate from the service layer.
 */
@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department entity) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Department toEntity(DepartmentRequest request) {
        return Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Department entity, DepartmentRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
    }
}
