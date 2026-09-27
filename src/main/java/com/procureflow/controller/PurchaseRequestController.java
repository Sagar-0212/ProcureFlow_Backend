package com.procureflow.controller;

import com.procureflow.dto.purchaserequest.PurchaseRequestRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestResponse;
import com.procureflow.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequestRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        PurchaseRequestResponse response = purchaseRequestService.create(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PurchaseRequestResponse>> getAllPurchaseRequests(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(purchaseRequestService.getAll(userEmail));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequestById(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(purchaseRequestService.getById(id, userEmail));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> updatePurchaseRequest(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(purchaseRequestService.update(id, request, userEmail));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePurchaseRequest(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        purchaseRequestService.delete(id, userEmail);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> submitPurchaseRequest(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(purchaseRequestService.submit(id, userEmail));
    }
}
