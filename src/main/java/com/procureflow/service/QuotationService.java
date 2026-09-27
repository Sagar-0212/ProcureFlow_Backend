package com.procureflow.service;

import com.procureflow.dto.quotation.*;
import com.procureflow.entity.*;
import com.procureflow.enums.QuotationStatus;
import com.procureflow.enums.RequestStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.QuotationMapper;
import com.procureflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final QuotationMapper quotationMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public QuotationResponse create(QuotationRequest request) {
        PurchaseRequest pr = purchaseRequestRepository.findById(request.getPurchaseRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", request.getPurchaseRequestId()));

        if (pr.getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException("Quotations can only be created for APPROVED Purchase Requests");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        List<QuotationStatus> activeStatuses = List.of(QuotationStatus.DRAFT, QuotationStatus.SUBMITTED, QuotationStatus.SELECTED);
        if (quotationRepository.existsByPurchaseRequestIdAndSupplierIdAndStatusIn(pr.getId(), supplier.getId(), activeStatuses)) {
            throw new BadRequestException("A quotation for this supplier already exists for the specified Purchase Request");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Quotation must contain at least one item");
        }

        if (request.getValidUntil() != null && request.getValidUntil().isBefore(request.getQuotationDate())) {
            throw new BadRequestException("Valid until date cannot be before quotation date");
        }

        String quotationNumber = generateUniqueQuotationNumber();

        Quotation quotation = Quotation.builder()
                .quotationNumber(quotationNumber)
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(request.getQuotationDate())
                .validUntil(request.getValidUntil())
                .paymentTerms(request.getPaymentTerms())
                .deliveryDays(request.getDeliveryDays())
                .status(QuotationStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal grandTotal = processAndAddItems(quotation, request.getItems());
        quotation.setTotalAmount(grandTotal);

        Quotation savedQuotation = quotationRepository.save(quotation);
        log.info("Created quotation id={} number='{}' for PR id={} supplier id={}", savedQuotation.getId(), savedQuotation.getQuotationNumber(), pr.getId(), supplier.getId());

        return quotationMapper.toResponse(savedQuotation);
    }

    @Transactional(readOnly = true)
    public QuotationResponse getById(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", id));
        return quotationMapper.toResponse(quotation);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> getAll() {
        return quotationRepository.findAll()
                .stream()
                .map(quotationMapper::toResponse)
                .toList();
    }

    @Transactional
    public QuotationResponse update(Long id, QuotationRequest request) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", id));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException("Cannot edit quotation in status: " + quotation.getStatus());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Quotation must contain at least one item");
        }

        if (request.getValidUntil() != null && request.getValidUntil().isBefore(request.getQuotationDate())) {
            throw new BadRequestException("Valid until date cannot be before quotation date");
        }

        quotation.setQuotationDate(request.getQuotationDate());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setPaymentTerms(request.getPaymentTerms());
        quotation.setDeliveryDays(request.getDeliveryDays());

        quotation.clearItems();
        BigDecimal grandTotal = processAndAddItems(quotation, request.getItems());
        quotation.setTotalAmount(grandTotal);

        Quotation updated = quotationRepository.save(quotation);
        log.info("Updated quotation id={} status='{}'", updated.getId(), updated.getStatus());

        return quotationMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", id));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException("Cannot delete quotation in status: " + quotation.getStatus());
        }

        quotationRepository.delete(quotation);
        log.info("Deleted DRAFT quotation id={}", id);
    }

    @Transactional
    public QuotationResponse submit(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", id));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT quotations can be submitted");
        }

        if (quotation.getItems() == null || quotation.getItems().isEmpty()) {
            throw new BadRequestException("Cannot submit an empty quotation");
        }

        quotation.setStatus(QuotationStatus.SUBMITTED);
        Quotation submitted = quotationRepository.save(quotation);
        log.info("Submitted quotation id={} number='{}' status='{}'", submitted.getId(), submitted.getQuotationNumber(), submitted.getStatus());

        return quotationMapper.toResponse(submitted);
    }

    @Transactional(readOnly = true)
    public QuotationComparisonResponse compare(Long purchaseRequestId) {
        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", purchaseRequestId));

        List<Quotation> quotations = quotationRepository.findByPurchaseRequestId(purchaseRequestId);

        List<QuotationResponse> quotationResponses = quotations.stream()
                .map(quotationMapper::toResponse)
                .toList();

        return QuotationComparisonResponse.builder()
                .purchaseRequestId(pr.getId())
                .purchaseRequestNumber(pr.getRequestNumber())
                .purchaseRequestTitle(pr.getTitle())
                .estimatedTotalAmount(pr.getTotalAmount())
                .quotations(quotationResponses)
                .build();
    }

    @Transactional
    public QuotationResponse select(Long quotationId) {
        Quotation selectedQuotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", quotationId));

        if (selectedQuotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED quotations can be selected");
        }

        Long prId = selectedQuotation.getPurchaseRequest().getId();
        if (quotationRepository.existsByPurchaseRequestIdAndStatus(prId, QuotationStatus.SELECTED)) {
            throw new BadRequestException("A quotation has already been selected for this Purchase Request");
        }

        // Change selected quotation status to SELECTED
        selectedQuotation.setStatus(QuotationStatus.SELECTED);
        quotationRepository.save(selectedQuotation);

        // Change all other quotations for the same PR to REJECTED
        List<Quotation> otherQuotations = quotationRepository.findByPurchaseRequestId(prId);
        for (Quotation other : otherQuotations) {
            if (!other.getId().equals(quotationId)) {
                other.setStatus(QuotationStatus.REJECTED);
                quotationRepository.save(other);
            }
        }

        log.info("Selected quotation id={} for PR id={}. Other quotations rejected.", quotationId, prId);

        auditLogService.logAction("SELECT_QUOTATION", "Quotation", quotationId, "Selected quotation " + selectedQuotation.getQuotationNumber() + " for PR id=" + prId);

        return quotationMapper.toResponse(selectedQuotation);
    }

    private BigDecimal processAndAddItems(Quotation quotation, List<QuotationItemRequest> itemRequests) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuotationItemRequest itemReq : itemRequests) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }

            if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Unit price must not be negative");
            }

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            QuotationItem item = QuotationItem.builder()
                    .quotation(quotation)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();

            quotation.addItem(item);
            total = total.add(itemTotal);
        }
        return total;
    }

    private String generateUniqueQuotationNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "QT-" + timestamp + "-" + randomHex;
    }
}
