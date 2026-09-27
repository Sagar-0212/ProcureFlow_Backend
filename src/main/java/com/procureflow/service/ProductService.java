package com.procureflow.service;

import com.procureflow.dto.product.ProductRequest;
import com.procureflow.dto.product.ProductResponse;
import com.procureflow.entity.Category;
import com.procureflow.entity.Product;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.ProductMapper;
import com.procureflow.repository.CategoryRepository;
import com.procureflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = productMapper.toEntity(request, category);
        Product saved = productRepository.save(product);
        log.info("Created product: id={}, sku='{}', name='{}'", saved.getId(), saved.getSku(), saved.getName());
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Check SKU uniqueness only if it's changing
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        productMapper.updateEntity(product, request, category);
        Product updated = productRepository.save(product);
        log.info("Updated product: id={}, sku='{}'", updated.getId(), updated.getSku());
        return productMapper.toResponse(updated);
    }

    @Transactional
    public ProductResponse toggleActive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(!product.isActive());
        Product saved = productRepository.save(product);
        log.info("Toggled product active status: id={}, active={}", saved.getId(), saved.isActive());
        return productMapper.toResponse(saved);
    }
}
