package com.procureflow.repository;

import com.procureflow.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByInvoiceNumber(String invoiceNumber);
    boolean existsBySupplierInvoiceNumberAndPurchaseOrderId(String supplierInvoiceNumber, Long purchaseOrderId);
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
