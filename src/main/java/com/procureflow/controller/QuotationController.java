package com.procureflow.controller;

import com.procureflow.dto.quotation.QuotationComparisonResponse;
import com.procureflow.dto.quotation.QuotationRequest;
import com.procureflow.dto.quotation.QuotationResponse;
import com.procureflow.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<QuotationResponse> createQuotation(@Valid @RequestBody QuotationRequest request) {
        QuotationResponse response = quotationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuotationResponse>> getAllQuotations() {
        return ResponseEntity.ok(quotationService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuotationResponse> getQuotationById(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<QuotationResponse> updateQuotation(
            @PathVariable Long id,
            @Valid @RequestBody QuotationRequest request) {
        return ResponseEntity.ok(quotationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> deleteQuotation(@PathVariable Long id) {
        quotationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<QuotationResponse> submitQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.submit(id));
    }

    @GetMapping("/compare/{purchaseRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'MANAGER')")
    public ResponseEntity<QuotationComparisonResponse> compareQuotations(@PathVariable Long purchaseRequestId) {
        return ResponseEntity.ok(quotationService.compare(purchaseRequestId));
    }

    @PostMapping("/{id}/select")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<QuotationResponse> selectQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.select(id));
    }
}
