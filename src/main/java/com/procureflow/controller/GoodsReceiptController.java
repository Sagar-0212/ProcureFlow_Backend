package com.procureflow.controller;

import com.procureflow.dto.receipt.CreateGoodsReceiptRequest;
import com.procureflow.dto.receipt.GoodsReceiptResponse;
import com.procureflow.service.GoodsReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<GoodsReceiptResponse> createGoodsReceipt(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        GoodsReceiptResponse response = goodsReceiptService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GoodsReceiptResponse>> getAllGoodsReceipts() {
        return ResponseEntity.ok(goodsReceiptService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GoodsReceiptResponse> getGoodsReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.getById(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<GoodsReceiptResponse> receiveGoodsReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.receive(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<GoodsReceiptResponse> cancelGoodsReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.cancel(id));
    }
}
