package com.procureflow.service;

import com.procureflow.dto.department.DepartmentRequest;
import com.procureflow.dto.department.DepartmentResponse;
import com.procureflow.entity.Department;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.DepartmentMapper;
import com.procureflow.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department", "name", request.getName());
        }
        Department department = departmentMapper.toEntity(request);
        Department saved = departmentRepository.save(department);
        log.info("Created department: id={}, name='{}'", saved.getId(), saved.getName());
        return departmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return departmentMapper.toResponse(department);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // Check uniqueness only if the name is changing
        if (!department.getName().equals(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department", "name", request.getName());
        }

        departmentMapper.updateEntity(department, request);
        Department updated = departmentRepository.save(department);
        log.info("Updated department: id={}, name='{}'", updated.getId(), updated.getName());
        return departmentMapper.toResponse(updated);
    }

    @Transactional
    public DepartmentResponse toggleActive(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        department.setActive(!department.isActive());
        Department saved = departmentRepository.save(department);
        log.info("Toggled department active status: id={}, active={}", saved.getId(), saved.isActive());
        return departmentMapper.toResponse(saved);
    }
}
