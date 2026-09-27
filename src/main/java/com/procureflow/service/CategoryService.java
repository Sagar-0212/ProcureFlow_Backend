package com.procureflow.service;

import com.procureflow.dto.category.CategoryRequest;
import com.procureflow.dto.category.CategoryResponse;
import com.procureflow.entity.Category;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.CategoryMapper;
import com.procureflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        log.info("Created category: id={}, name='{}'", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        categoryMapper.updateEntity(category, request);
        Category updated = categoryRepository.save(category);
        log.info("Updated category: id={}, name='{}'", updated.getId(), updated.getName());
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public CategoryResponse toggleActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setActive(!category.isActive());
        Category saved = categoryRepository.save(category);
        log.info("Toggled category active status: id={}, active={}", saved.getId(), saved.isActive());
        return categoryMapper.toResponse(saved);
    }
}
