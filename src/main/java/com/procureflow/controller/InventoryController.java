package com.procureflow.controller;

import com.procureflow.dto.inventory.InventoryAdjustmentRequest;
import com.procureflow.dto.inventory.InventoryResponse;
import com.procureflow.dto.inventory.InventoryTransactionResponse;
import com.procureflow.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @GetMapping("/{productId}/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventoryTransactionResponse>> getTransactionsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getTransactionsByProductId(productId));
    }

    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(productId, request));
    }
}
