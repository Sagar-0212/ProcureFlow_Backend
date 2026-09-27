package com.procureflow.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReportResponse {
    private long totalInvoices;
    private long totalInvoicesCount;
    private BigDecimal totalInvoicedAmount;
    private Map<String, Long> invoicesByStatus;

    private long totalPaymentsCount;
    private BigDecimal paidInvoiceAmount;
    private BigDecimal totalPaidAmount;
    private Map<String, Long> paymentsByMethod;
}
