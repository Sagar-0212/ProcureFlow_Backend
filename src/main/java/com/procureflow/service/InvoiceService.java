package com.procureflow.service;

import com.procureflow.dto.invoice.*;
import com.procureflow.entity.*;
import com.procureflow.enums.InvoiceStatus;
import com.procureflow.enums.POStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.InvoiceMapper;
import com.procureflow.repository.GoodsReceiptItemRepository;
import com.procureflow.repository.InvoiceRepository;
import com.procureflow.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final InvoiceMapper invoiceMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", request.getPurchaseOrderId()));

        if (po.getStatus() != POStatus.PARTIALLY_RECEIVED &&
                po.getStatus() != POStatus.FULLY_RECEIVED &&
                po.getStatus() != POStatus.COMPLETED) {
            throw new BadRequestException("Invoice can only be created for Purchase Orders in PARTIALLY_RECEIVED, FULLY_RECEIVED, or COMPLETED status");
        }

        if (request.getSupplierId() != null && !request.getSupplierId().equals(po.getSupplier().getId())) {
            throw new BadRequestException("Invoice supplier does not match Purchase Order supplier");
        }

        if (request.getSupplierInvoiceNumber() != null && !request.getSupplierInvoiceNumber().isBlank() &&
                invoiceRepository.existsBySupplierInvoiceNumberAndPurchaseOrderId(request.getSupplierInvoiceNumber(), po.getId())) {
            throw new DuplicateResourceException("Invoice with supplier invoice number '" + request.getSupplierInvoiceNumber() + "' already exists for this PO");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Invoice must contain at least one item");
        }

        String invoiceNumber = generateUniqueInvoiceNumber();
        LocalDate invoiceDate = request.getInvoiceDate();
        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : invoiceDate.plusDays(30);
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .supplierInvoiceNumber(request.getSupplierInvoiceNumber())
                .purchaseOrder(po)
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(taxAmount)
                .totalAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING_VERIFICATION)
                .notes(request.getNotes())
                .build();

        processAndAddItems(invoice, po, request.getItems());
        recalculateTotals(invoice);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Created Invoice id={} number='{}' for PO id={}", savedInvoice.getId(), savedInvoice.getInvoiceNumber(), po.getId());

        // Perform Three-Way Matching
        performThreeWayMatchInternal(savedInvoice);

        Invoice updatedInvoice = invoiceRepository.save(savedInvoice);
        logMatchAudit(updatedInvoice);
        return invoiceMapper.toResponse(updatedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAll()
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Transactional
    public InvoiceResponse update(Long id, CreateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.APPROVED || invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Cannot edit invoice in status: " + invoice.getStatus());
        }

        if (request.getSupplierInvoiceNumber() != null) {
            invoice.setSupplierInvoiceNumber(request.getSupplierInvoiceNumber());
        }
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getTaxAmount() != null) {
            invoice.setTaxAmount(request.getTaxAmount());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            invoice.clearItems();
            processAndAddItems(invoice, invoice.getPurchaseOrder(), request.getItems());
            recalculateTotals(invoice);
        }

        performThreeWayMatchInternal(invoice);

        Invoice updated = invoiceRepository.save(invoice);
        log.info("Updated Invoice id={} status='{}'", updated.getId(), updated.getStatus());
        logMatchAudit(updated);

        return invoiceMapper.toResponse(updated);
    }

    @Transactional
    public InvoiceResponse verify(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        performThreeWayMatchInternal(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Verified Invoice id={} matchStatus='{}'", saved.getId(), saved.getStatus());
        logMatchAudit(saved);

        return invoiceMapper.toResponse(saved);
    }

    @Transactional
    public InvoiceResponse approve(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() != InvoiceStatus.MATCHED) {
            throw new BadRequestException("Invoice can only be approved after a successful three-way match (current status: " + invoice.getStatus() + ")");
        }

        invoice.setStatus(InvoiceStatus.APPROVED);
        Invoice approved = invoiceRepository.save(invoice);
        log.info("Approved Invoice id={}", approved.getId());

        auditLogService.logAction("APPROVE_INVOICE", "Invoice", approved.getId(), "Approved Invoice #" + approved.getInvoiceNumber());

        return invoiceMapper.toResponse(approved);
    }

    @Transactional
    public InvoiceResponse reject(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        invoice.setStatus(InvoiceStatus.REJECTED);
        Invoice rejected = invoiceRepository.save(invoice);
        log.info("Rejected Invoice id={}", rejected.getId());

        auditLogService.logAction("REJECT_INVOICE", "Invoice", rejected.getId(), "Rejected Invoice #" + rejected.getInvoiceNumber());

        return invoiceMapper.toResponse(rejected);
    }

    private void logMatchAudit(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.MATCHED) {
            auditLogService.logAction("MATCH_INVOICE", "Invoice", invoice.getId(), "Matched Invoice #" + invoice.getInvoiceNumber());
        } else if (invoice.getStatus() == InvoiceStatus.MISMATCH) {
            auditLogService.logAction("MISMATCH_INVOICE", "Invoice", invoice.getId(), "Mismatch detected for Invoice #" + invoice.getInvoiceNumber());
        }
    }

    @Transactional(readOnly = true)
    public ThreeWayMatchResponse getMatchDetails(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        return buildThreeWayMatchResponse(invoice);
    }

    private void performThreeWayMatchInternal(Invoice invoice) {
        ThreeWayMatchResponse matchResult = buildThreeWayMatchResponse(invoice);
        if (matchResult.isMatched()) {
            invoice.setStatus(InvoiceStatus.MATCHED);
            invoice.setMismatchReason(null);
        } else {
            invoice.setStatus(InvoiceStatus.MISMATCH);
            invoice.setMismatchReason(matchResult.getMismatchReason());
        }
    }

    private ThreeWayMatchResponse buildThreeWayMatchResponse(Invoice invoice) {
        PurchaseOrder po = invoice.getPurchaseOrder();
        List<ThreeWayMatchResponse.ItemMatchResult> itemResults = new ArrayList<>();
        List<String> mismatchMessages = new ArrayList<>();
        boolean overallMatch = true;

        for (InvoiceItem item : invoice.getItems()) {
            PurchaseOrderItem poItem = item.getPurchaseOrderItem();
            String productName = poItem.getProduct() != null ? poItem.getProduct().getName() : "Product #" + poItem.getId();

            Integer poQty = poItem.getQuantity();
            BigDecimal poPrice = poItem.getUnitPrice();
            Integer acceptedQty = goodsReceiptItemRepository.sumAcceptedQuantityByPurchaseOrderItemId(poItem.getId());

            Integer invQty = item.getQuantity();
            BigDecimal invPrice = item.getUnitPrice();

            boolean qtyMatched = invQty <= acceptedQty;
            boolean priceMatched = invPrice.compareTo(poPrice) == 0;

            List<String> itemMismatches = new ArrayList<>();
            if (!qtyMatched) {
                overallMatch = false;
                itemMismatches.add("Invoiced quantity (" + invQty + ") exceeds accepted Goods Receipt quantity (" + acceptedQty + ")");
            }
            if (!priceMatched) {
                overallMatch = false;
                itemMismatches.add("Invoiced unit price (" + invPrice + ") differs from PO unit price (" + poPrice + ")");
            }

            String mismatchDetails = itemMismatches.isEmpty() ? null : String.join("; ", itemMismatches);
            if (mismatchDetails != null) {
                mismatchMessages.add(productName + ": " + mismatchDetails);
            }

            itemResults.add(ThreeWayMatchResponse.ItemMatchResult.builder()
                    .invoiceItemId(item.getId())
                    .purchaseOrderItemId(poItem.getId())
                    .productName(productName)
                    .poOrderedQuantity(poQty)
                    .acceptedGoodsReceiptQuantity(acceptedQty)
                    .invoicedQuantity(invQty)
                    .poUnitPrice(poPrice)
                    .invoicedUnitPrice(invPrice)
                    .quantityMatched(qtyMatched)
                    .priceMatched(priceMatched)
                    .mismatchDetails(mismatchDetails)
                    .build());
        }

        String combinedMismatchReason = mismatchMessages.isEmpty() ? null : String.join(" | ", mismatchMessages);

        return ThreeWayMatchResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .purchaseOrderId(po.getId())
                .purchaseOrderNumber(po.getPoNumber())
                .matchStatus(overallMatch ? InvoiceStatus.MATCHED : InvoiceStatus.MISMATCH)
                .mismatchReason(combinedMismatchReason)
                .matched(overallMatch)
                .itemResults(itemResults)
                .build();
    }

    private void processAndAddItems(Invoice invoice, PurchaseOrder po, List<InvoiceItemRequest> itemRequests) {
        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(java.util.stream.Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        for (InvoiceItemRequest itemReq : itemRequests) {
            PurchaseOrderItem poItem = poItemMap.get(itemReq.getPurchaseOrderItemId());
            if (poItem == null) {
                throw new BadRequestException("PO item id=" + itemReq.getPurchaseOrderItemId() + " does not belong to Purchase Order id=" + po.getId());
            }

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }

            if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Unit price cannot be negative");
            }

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .purchaseOrderItem(poItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();

            invoice.addItem(invoiceItem);
        }
    }

    private void recalculateTotals(Invoice invoice) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                subtotal = subtotal.add(item.getTotalPrice());
            }
        }
        invoice.setSubtotal(subtotal);
        BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
        invoice.setTotalAmount(subtotal.add(tax));
    }

    private String generateUniqueInvoiceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "INV-" + timestamp + "-" + randomHex;
    }
}
