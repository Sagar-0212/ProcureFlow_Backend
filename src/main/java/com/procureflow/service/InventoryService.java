package com.procureflow.service;

import com.procureflow.dto.inventory.InventoryAdjustmentRequest;
import com.procureflow.dto.inventory.InventoryResponse;
import com.procureflow.dto.inventory.InventoryTransactionResponse;
import com.procureflow.entity.*;
import com.procureflow.enums.GoodsReceiptStatus;
import com.procureflow.enums.InventoryTransactionType;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.InventoryMapper;
import com.procureflow.repository.InventoryRepository;
import com.procureflow.repository.InventoryTransactionRepository;
import com.procureflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createEmptyInventory(product));

        return inventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionResponse> getTransactionsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        return inventoryTransactionRepository.findByProductIdOrderByTransactionDateDesc(productId)
                .stream()
                .map(inventoryMapper::toTransactionResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse adjustStock(Long productId, InventoryAdjustmentRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (request.getQuantityChange() == null || request.getQuantityChange() == 0) {
            throw new BadRequestException("Quantity change must not be zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createEmptyInventory(product));

        int currentQty = inventory.getQuantity();
        int newBalance = currentQty + request.getQuantityChange();

        if (newBalance < 0) {
            throw new BadRequestException("Inventory adjustment would result in negative balance (" + newBalance + ")");
        }

        inventory.setQuantity(newBalance);
        inventory.setLastUpdated(LocalDateTime.now());
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .transactionType(InventoryTransactionType.ADJUSTMENT)
                .quantity(request.getQuantityChange())
                .referenceType("MANUAL_ADJUSTMENT")
                .referenceId(null)
                .balanceAfter(newBalance)
                .notes(request.getNotes() != null ? request.getNotes() : "Manual stock adjustment")
                .transactionDate(LocalDateTime.now())
                .build();

        inventoryTransactionRepository.save(transaction);
        log.info("Adjusted inventory for product id={} by {} to new balance {}", productId, request.getQuantityChange(), newBalance);

        auditLogService.logAction("ADJUST_INVENTORY", "Inventory", savedInventory.getId(),
                "Adjusted stock for product '" + product.getName() + "' by " + request.getQuantityChange() + " (new balance: " + newBalance + ")");

        return inventoryMapper.toResponse(savedInventory);
    }

    @Transactional
    public void processGoodsReceipt(GoodsReceipt goodsReceipt) {
        if (goodsReceipt == null || goodsReceipt.getStatus() != GoodsReceiptStatus.RECEIVED) {
            log.info("Skipping inventory update for non-RECEIVED GoodsReceipt id={}", goodsReceipt != null ? goodsReceipt.getId() : null);
            return;
        }

        if (inventoryTransactionRepository.existsByReferenceTypeAndReferenceId("GOODS_RECEIPT", goodsReceipt.getId())) {
            log.info("GoodsReceipt id={} has already been processed for inventory", goodsReceipt.getId());
            return;
        }

        if (goodsReceipt.getItems() == null || goodsReceipt.getItems().isEmpty()) {
            return;
        }

        for (GoodsReceiptItem item : goodsReceipt.getItems()) {
            int accepted = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
            if (accepted <= 0) {
                continue; // Rejected quantity does not increase inventory
            }

            Product product = item.getPurchaseOrderItem().getProduct();
            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseGet(() -> createEmptyInventory(product));

            int currentQty = inventory.getQuantity();
            int newBalance = currentQty + accepted;

            inventory.setQuantity(newBalance);
            inventory.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .product(product)
                    .transactionType(InventoryTransactionType.RECEIPT)
                    .quantity(accepted)
                    .referenceType("GOODS_RECEIPT")
                    .referenceId(goodsReceipt.getId())
                    .balanceAfter(newBalance)
                    .notes("Accepted goods from GoodsReceipt #" + goodsReceipt.getReceiptNumber())
                    .transactionDate(LocalDateTime.now())
                    .build();

            inventoryTransactionRepository.save(transaction);
            log.info("Processed GoodsReceipt id={}: Product id={} stock updated +{} (new balance: {})",
                    goodsReceipt.getId(), product.getId(), accepted, newBalance);
        }
    }

    private Inventory createEmptyInventory(Product product) {
        Inventory newInventory = Inventory.builder()
                .product(product)
                .quantity(0)
                .lastUpdated(LocalDateTime.now())
                .build();
        return inventoryRepository.save(newInventory);
    }
}
