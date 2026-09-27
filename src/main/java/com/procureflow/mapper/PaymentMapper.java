package com.procureflow.mapper;

import com.procureflow.dto.payment.PaymentResponse;
import com.procureflow.entity.Invoice;
import com.procureflow.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        Invoice invoice = payment.getInvoice();
        Long invoiceId = invoice != null ? invoice.getId() : null;
        String invoiceNumber = invoice != null ? invoice.getInvoiceNumber() : null;

        Long poId = (invoice != null && invoice.getPurchaseOrder() != null) ? invoice.getPurchaseOrder().getId() : null;
        String poNumber = (invoice != null && invoice.getPurchaseOrder() != null) ? invoice.getPurchaseOrder().getPoNumber() : null;

        Long supplierId = (invoice != null && invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null) ?
                invoice.getPurchaseOrder().getSupplier().getId() : null;
        String supplierName = (invoice != null && invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null) ?
                invoice.getPurchaseOrder().getSupplier().getCompanyName() : null;

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .purchaseOrderId(poId)
                .purchaseOrderNumber(poNumber)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .status(payment.getStatus())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
