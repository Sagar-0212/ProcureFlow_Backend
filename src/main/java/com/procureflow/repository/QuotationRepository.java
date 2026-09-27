package com.procureflow.repository;

import com.procureflow.entity.Quotation;
import com.procureflow.enums.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByQuotationNumber(String quotationNumber);

    boolean existsByQuotationNumber(String quotationNumber);

    List<Quotation> findByPurchaseRequestId(Long purchaseRequestId);

    List<Quotation> findByPurchaseRequestIdAndStatusIn(Long purchaseRequestId, List<QuotationStatus> statuses);

    boolean existsByPurchaseRequestIdAndSupplierIdAndStatusIn(Long purchaseRequestId, Long supplierId, List<QuotationStatus> statuses);

    boolean existsByPurchaseRequestIdAndStatus(Long purchaseRequestId, QuotationStatus status);
}
