package com.procureflow.service;

import com.procureflow.dto.report.*;
import com.procureflow.entity.*;
import com.procureflow.enums.PaymentStatus;
import com.procureflow.enums.QuotationStatus;
import com.procureflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final QuotationRepository quotationRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PurchaseReportResponse getPurchasesReport() {
        List<PurchaseRequest> prs = purchaseRequestRepository.findAll();
        long totalPRCount = prs.size();
        BigDecimal totalPRAmount = prs.stream()
                .map(pr -> pr.getTotalAmount() != null ? pr.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> requestsByStatus = prs.stream()
                .collect(Collectors.groupingBy(pr -> pr.getStatus().name(), Collectors.counting()));

        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
        long totalPOCount = pos.size();
        BigDecimal totalPOAmount = pos.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> ordersByStatus = pos.stream()
                .collect(Collectors.groupingBy(po -> po.getStatus().name(), Collectors.counting()));

        return PurchaseReportResponse.builder()
                .totalRequests(totalPRCount)
                .totalPRCount(totalPRCount)
                .totalPROrderedAmount(totalPRAmount)
                .requestsByStatus(requestsByStatus)
                .totalPOCount(totalPOCount)
                .totalPOAmount(totalPOAmount)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public SupplierReportResponse getSuppliersReport() {
        List<Supplier> suppliers = supplierRepository.findAll();
        long totalSuppliers = suppliers.size();
        long activeSuppliers = suppliers.stream().filter(Supplier::isActive).count();

        List<SupplierReportResponse.SupplierPerformanceSummary> summaries = new ArrayList<>();

        for (Supplier supplier : suppliers) {
            List<Quotation> quotations = quotationRepository.findAll().stream()
                    .filter(q -> q.getSupplier().getId().equals(supplier.getId()))
                    .toList();

            long submitted = quotations.stream().filter(q -> q.getStatus() != QuotationStatus.DRAFT).count();
            long selected = quotations.stream().filter(q -> q.getStatus() == QuotationStatus.SELECTED).count();

            List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplier.getId());
            long poCount = pos.size();
            BigDecimal totalPOAmount = pos.stream()
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summaries.add(SupplierReportResponse.SupplierPerformanceSummary.builder()
                    .supplierId(supplier.getId())
                    .supplierCode(supplier.getSupplierCode())
                    .companyName(supplier.getCompanyName())
                    .totalQuotationsSubmitted(submitted)
                    .totalQuotationsSelected(selected)
                    .totalPOsIssued(poCount)
                    .totalPOAmount(totalPOAmount)
                    .build());
        }

        return SupplierReportResponse.builder()
                .totalSuppliers(totalSuppliers)
                .totalSuppliersCount(totalSuppliers)
                .activeSuppliers(activeSuppliers)
                .activeSuppliersCount(activeSuppliers)
                .supplierSummaries(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport() {
        List<Product> products = productRepository.findAll();
        long totalProductsTracked = products.size();
        long totalStockUnits = 0;
        long lowStockCount = 0;

        List<InventoryReportResponse.ProductInventorySummary> summaries = new ArrayList<>();

        for (Product product : products) {
            Inventory inv = inventoryRepository.findByProductId(product.getId()).orElse(null);
            int currentStock = inv != null ? inv.getQuantity() : 0;
            int minStock = product.getMinimumStockLevel() != null ? product.getMinimumStockLevel() : 10;
            boolean isLowStock = currentStock <= minStock;

            totalStockUnits += currentStock;
            if (isLowStock) {
                lowStockCount++;
            }

            summaries.add(InventoryReportResponse.ProductInventorySummary.builder()
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .currentStock(currentStock)
                    .minimumStockLevel(minStock)
                    .isLowStock(isLowStock)
                    .build());
        }

        return InventoryReportResponse.builder()
                .totalItems(totalProductsTracked)
                .totalProductsTracked(totalProductsTracked)
                .totalStockUnits(totalStockUnits)
                .lowStockCount(lowStockCount)
                .items(summaries)
                .productSummaries(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentReportResponse getPaymentsReport() {
        List<Invoice> invoices = invoiceRepository.findAll();
        long totalInvoicesCount = invoices.size();
        BigDecimal totalInvoicedAmount = invoices.stream()
                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> invoicesByStatus = invoices.stream()
                .collect(Collectors.groupingBy(inv -> inv.getStatus().name(), Collectors.counting()));

        List<Payment> payments = paymentRepository.findAll();
        long totalPaymentsCount = payments.size();
        BigDecimal totalPaidAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> paymentsByMethod = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentMethod().name(), Collectors.counting()));

        return PaymentReportResponse.builder()
                .totalInvoices(totalInvoicesCount)
                .totalInvoicesCount(totalInvoicesCount)
                .totalInvoicedAmount(totalInvoicedAmount)
                .invoicesByStatus(invoicesByStatus)
                .totalPaymentsCount(totalPaymentsCount)
                .paidInvoiceAmount(totalPaidAmount)
                .totalPaidAmount(totalPaidAmount)
                .paymentsByMethod(paymentsByMethod)
                .build();
    }
}
