package com.procureflow.service;

import com.procureflow.dto.supplier.SupplierRequest;
import com.procureflow.dto.supplier.SupplierResponse;
import com.procureflow.entity.Supplier;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.SupplierMapper;
import com.procureflow.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new DuplicateResourceException("Supplier", "supplierCode", request.getSupplierCode());
        }
        Supplier supplier = supplierMapper.toEntity(request);
        Supplier saved = supplierRepository.save(supplier);
        log.info("Created supplier: id={}, code='{}'", saved.getId(), saved.getSupplierCode());
        return supplierMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        if (!supplier.getSupplierCode().equals(request.getSupplierCode())
                && supplierRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new DuplicateResourceException("Supplier", "supplierCode", request.getSupplierCode());
        }

        supplierMapper.updateEntity(supplier, request);
        Supplier updated = supplierRepository.save(supplier);
        log.info("Updated supplier: id={}, code='{}'", updated.getId(), updated.getSupplierCode());
        return supplierMapper.toResponse(updated);
    }

    @Transactional
    public SupplierResponse toggleActive(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        supplier.setActive(!supplier.isActive());
        Supplier saved = supplierRepository.save(supplier);
        log.info("Toggled supplier active status: id={}, active={}", saved.getId(), saved.isActive());
        return supplierMapper.toResponse(saved);
    }
}
