package com.procureflow.service;

import com.procureflow.dto.payment.CreatePaymentRequest;
import com.procureflow.dto.payment.PaymentResponse;
import com.procureflow.entity.Invoice;
import com.procureflow.entity.Payment;
import com.procureflow.enums.InvoiceStatus;
import com.procureflow.enums.PaymentStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.PaymentMapper;
import com.procureflow.repository.InvoiceRepository;
import com.procureflow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        List<PaymentStatus> activeStatuses = List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED);
        if (invoice.getStatus() == InvoiceStatus.PAID || paymentRepository.existsByInvoiceIdAndStatusIn(invoice.getId(), activeStatuses)) {
            throw new DuplicateResourceException("An active payment already exists for this invoice");
        }

        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new BadRequestException("Payment can only be created for APPROVED invoices (current status: " + invoice.getStatus() + ")");
        }

        if (request.getAmount().compareTo(invoice.getTotalAmount()) != 0) {
            throw new BadRequestException("Payment amount (" + request.getAmount() + ") must equal invoice total (" + invoice.getTotalAmount() + ")");
        }

        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            if (paymentRepository.existsByTransactionReferenceAndStatusNot(request.getTransactionReference(), PaymentStatus.CANCELLED)) {
                throw new DuplicateResourceException("Transaction reference '" + request.getTransactionReference() + "' already exists");
            }
        }

        PaymentStatus initialStatus = request.getStatus() != null ? request.getStatus() : PaymentStatus.COMPLETED;
        String paymentNumber = generateUniquePaymentNumber();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .invoice(invoice)
                .paymentDate(request.getPaymentDate())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .status(initialStatus)
                .notes(request.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created Payment id={} paymentNumber='{}' for invoice id={}", savedPayment.getId(), savedPayment.getPaymentNumber(), invoice.getId());

        if (savedPayment.getStatus() == PaymentStatus.COMPLETED) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            log.info("Updated Invoice id={} status to PAID", invoice.getId());
            auditLogService.logAction("COMPLETE_PAYMENT", "Payment", savedPayment.getId(), "Completed Payment #" + savedPayment.getPaymentNumber() + " of amount " + savedPayment.getAmount());
        }

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse complete(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return paymentMapper.toResponse(payment);
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.FAILED) {
            throw new BadRequestException("Cannot complete a payment in status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        Payment completedPayment = paymentRepository.save(payment);

        Invoice invoice = payment.getInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        log.info("Completed Payment id={} and updated Invoice id={} status to PAID", completedPayment.getId(), invoice.getId());

        auditLogService.logAction("COMPLETE_PAYMENT", "Payment", completedPayment.getId(), "Completed Payment #" + completedPayment.getPaymentNumber() + " of amount " + completedPayment.getAmount());

        return paymentMapper.toResponse(completedPayment);
    }

    @Transactional
    public PaymentResponse cancel(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a COMPLETED payment");
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return paymentMapper.toResponse(payment);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment cancelledPayment = paymentRepository.save(payment);
        log.info("Cancelled Payment id={}", cancelledPayment.getId());

        auditLogService.logAction("CANCEL_PAYMENT", "Payment", cancelledPayment.getId(), "Cancelled Payment #" + cancelledPayment.getPaymentNumber());

        return paymentMapper.toResponse(cancelledPayment);
    }

    private String generateUniquePaymentNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PAY-" + timestamp + "-" + randomHex;
    }
}
