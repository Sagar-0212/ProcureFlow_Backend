package com.procureflow.service;

import com.procureflow.dto.dashboard.DashboardSummaryResponse;
import com.procureflow.entity.*;
import com.procureflow.enums.*;
import com.procureflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalPR = purchaseRequestRepository.count();
        long pendingPR = purchaseRequestRepository.findAll().stream()
                .filter(pr -> pr.getStatus() == RequestStatus.PENDING_APPROVAL)
                .count();
        long approvedPR = purchaseRequestRepository.findAll().stream()
                .filter(pr -> pr.getStatus() == RequestStatus.APPROVED)
                .count();

        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
        long totalPO = pos.size();
        long pendingPO = pos.stream().filter(po -> po.getStatus() == POStatus.DRAFT || po.getStatus() == POStatus.PENDING_APPROVAL).count();
        long partialPO = pos.stream().filter(po -> po.getStatus() == POStatus.PARTIALLY_RECEIVED).count();
        long fullPO = pos.stream().filter(po -> po.getStatus() == POStatus.FULLY_RECEIVED || po.getStatus() == POStatus.COMPLETED).count();

        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal totalInvoiceAmount = invoices.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.REJECTED)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingInvoices = invoices.stream().filter(inv -> inv.getStatus() == InvoiceStatus.PENDING_VERIFICATION).count();
        long mismatchedInvoices = invoices.stream().filter(inv -> inv.getStatus() == InvoiceStatus.MISMATCH).count();

        BigDecimal paidInvoiceAmount = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalInventoryItems = inventoryRepository.count();

        List<DashboardSummaryResponse.LowStockProductDto> lowStockProducts = new ArrayList<>();
        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            Inventory inv = inventoryRepository.findByProductId(product.getId()).orElse(null);
            int currentStock = inv != null ? inv.getQuantity() : 0;
            int minStock = product.getMinimumStockLevel() != null ? product.getMinimumStockLevel() : 10;

            if (currentStock <= minStock) {
                lowStockProducts.add(DashboardSummaryResponse.LowStockProductDto.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .productSku(product.getSku())
                        .currentStock(currentStock)
                        .minimumStockLevel(minStock)
                        .build());
            }
        }

        return DashboardSummaryResponse.builder()
                .totalPurchaseRequests(totalPR)
                .pendingApprovals(pendingPR)
                .approvedRequests(approvedPR)
                .totalPurchaseOrders(totalPO)
                .pendingPOCount(pendingPO)
                .partialPOCount(partialPO)
                .fullPOCount(fullPO)
                .totalInvoiceAmount(totalInvoiceAmount)
                .pendingInvoices(pendingInvoices)
                .pendingInvoicesCount(pendingInvoices)
                .mismatchedInvoices(mismatchedInvoices)
                .mismatchedInvoicesCount(mismatchedInvoices)
                .paidInvoiceAmount(paidInvoiceAmount)
                .totalInventoryItems(totalInventoryItems)
                .totalInventoryItemsCount(totalInventoryItems)
                .lowStockProducts(lowStockProducts)
                .build();
    }
}
