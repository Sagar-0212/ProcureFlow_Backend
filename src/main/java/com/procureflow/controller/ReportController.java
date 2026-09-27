package com.procureflow.controller;

import com.procureflow.dto.report.*;
import com.procureflow.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/purchases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseReportResponse> getPurchasesReport() {
        return ResponseEntity.ok(reportService.getPurchasesReport());
    }

    @GetMapping("/suppliers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupplierReportResponse> getSuppliersReport() {
        return ResponseEntity.ok(reportService.getSuppliersReport());
    }

    @GetMapping("/inventory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventoryReportResponse> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport());
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentReportResponse> getPaymentsReport() {
        return ResponseEntity.ok(reportService.getPaymentsReport());
    }
}
