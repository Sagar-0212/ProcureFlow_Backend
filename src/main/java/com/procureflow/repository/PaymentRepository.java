package com.procureflow.repository;

import com.procureflow.entity.Payment;
import com.procureflow.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByPaymentNumber(String paymentNumber);
    boolean existsByInvoiceIdAndStatusIn(Long invoiceId, List<PaymentStatus> statuses);
    boolean existsByTransactionReferenceAndStatusNot(String transactionReference, PaymentStatus status);
    List<Payment> findByInvoiceId(Long invoiceId);
    Optional<Payment> findByPaymentNumber(String paymentNumber);
}
