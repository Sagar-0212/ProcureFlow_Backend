package com.procureflow.service;

import com.procureflow.dto.po.CreatePurchaseOrderRequest;
import com.procureflow.dto.po.PurchaseOrderItemRequest;
import com.procureflow.dto.po.PurchaseOrderResponse;
import com.procureflow.entity.*;
import com.procureflow.enums.POStatus;
import com.procureflow.enums.QuotationStatus;
import com.procureflow.enums.RequestStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.DuplicateResourceException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.PurchaseOrderMapper;
import com.procureflow.repository.ProductRepository;
import com.procureflow.repository.PurchaseOrderRepository;
import com.procureflow.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final QuotationRepository quotationRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        Quotation quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", request.getQuotationId()));

        if (quotation.getStatus() != QuotationStatus.SELECTED) {
            throw new BadRequestException("Purchase Orders can only be created from a SELECTED quotation");
        }

        if (quotation.getPurchaseRequest().getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException("Selected quotation must belong to an APPROVED Purchase Request");
        }

        if (purchaseOrderRepository.existsByQuotationIdAndStatusNot(quotation.getId(), POStatus.CANCELLED)) {
            throw new DuplicateResourceException("Purchase order already exists for this quotation");
        }

        if (request.getExpectedDeliveryDate() != null && request.getExpectedDeliveryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expected delivery date cannot be in the past");
        }

        String poNumber = generateUniquePoNumber();
        LocalDate orderDate = LocalDate.now();
        LocalDate expectedDeliveryDate = request.getExpectedDeliveryDate() != null ?
                request.getExpectedDeliveryDate() :
                orderDate.plusDays(quotation.getDeliveryDays() != null ? quotation.getDeliveryDays() : 14);

        String paymentTerms = request.getPaymentTerms() != null && !request.getPaymentTerms().isBlank() ?
                request.getPaymentTerms() : quotation.getPaymentTerms();

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .purchaseRequest(quotation.getPurchaseRequest())
                .quotation(quotation)
                .supplier(quotation.getSupplier())
                .orderDate(orderDate)
                .expectedDeliveryDate(expectedDeliveryDate)
                .paymentTerms(paymentTerms)
                .status(POStatus.DRAFT)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .notes(request.getNotes())
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            processAndAddCustomItems(po, request.getItems());
        } else {
            copyItemsFromQuotation(po, quotation);
        }

        if (po.getItems() == null || po.getItems().isEmpty()) {
            throw new BadRequestException("Purchase order must contain at least one item");
        }

        recalculateTotals(po);

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);
        log.info("Created PurchaseOrder id={} poNumber='{}' from quotation id={}", savedPo.getId(), savedPo.getPoNumber(), quotation.getId());

        auditLogService.logAction("CREATE_PO", "PurchaseOrder", savedPo.getId(), "Created PO " + savedPo.getPoNumber());

        return purchaseOrderMapper.toResponse(savedPo);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        return purchaseOrderMapper.toResponse(po);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll() {
        return purchaseOrderRepository.findAll()
                .stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, CreatePurchaseOrderRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new BadRequestException("Cannot edit Purchase Order in status: " + po.getStatus());
        }

        if (request.getExpectedDeliveryDate() != null && request.getExpectedDeliveryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expected delivery date cannot be in the past");
        }

        if (request.getExpectedDeliveryDate() != null) {
            po.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        }
        if (request.getPaymentTerms() != null) {
            po.setPaymentTerms(request.getPaymentTerms());
        }
        if (request.getNotes() != null) {
            po.setNotes(request.getNotes());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            po.clearItems();
            processAndAddCustomItems(po, request.getItems());
            recalculateTotals(po);
        }

        PurchaseOrder updatedPo = purchaseOrderRepository.save(po);
        log.info("Updated DRAFT PurchaseOrder id={}", updatedPo.getId());

        return purchaseOrderMapper.toResponse(updatedPo);
    }

    @Transactional
    public void delete(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new BadRequestException("Cannot delete Purchase Order in status: " + po.getStatus());
        }

        purchaseOrderRepository.delete(po);
        log.info("Deleted DRAFT PurchaseOrder id={}", id);
    }

    @Transactional
    public PurchaseOrderResponse submit(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase orders can be submitted");
        }

        if (po.getItems() == null || po.getItems().isEmpty()) {
            throw new BadRequestException("Cannot submit a purchase order with no items");
        }

        po.setStatus(POStatus.PENDING_APPROVAL);
        PurchaseOrder submitted = purchaseOrderRepository.save(po);
        log.info("Submitted PurchaseOrder id={} status='{}'", submitted.getId(), submitted.getStatus());

        return purchaseOrderMapper.toResponse(submitted);
    }

    @Transactional
    public PurchaseOrderResponse approve(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != POStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only PENDING_APPROVAL purchase orders can be approved");
        }

        po.setStatus(POStatus.APPROVED);
        PurchaseOrder approved = purchaseOrderRepository.save(po);
        log.info("Approved PurchaseOrder id={} status='{}'", approved.getId(), approved.getStatus());

        auditLogService.logAction("APPROVE_PO", "PurchaseOrder", approved.getId(), "Approved PO " + approved.getPoNumber());

        return purchaseOrderMapper.toResponse(approved);
    }

    @Transactional
    public PurchaseOrderResponse send(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != POStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED purchase orders can be sent to supplier");
        }

        po.setStatus(POStatus.SENT_TO_SUPPLIER);
        PurchaseOrder sent = purchaseOrderRepository.save(po);
        log.info("Sent PurchaseOrder id={} to supplier. Status='{}'", sent.getId(), sent.getStatus());

        auditLogService.logAction("SEND_PO", "PurchaseOrder", sent.getId(), "Sent PO " + sent.getPoNumber() + " to supplier");

        return purchaseOrderMapper.toResponse(sent);
    }

    @Transactional(readOnly = true)
    public String generatePurchaseOrderTextPdf(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("                 PURCHASE ORDER                     \n");
        sb.append("====================================================\n");
        sb.append("PO Number: ").append(po.getPoNumber()).append("\n");
        sb.append("Date: ").append(po.getOrderDate()).append("\n");
        sb.append("Expected Delivery: ").append(po.getExpectedDeliveryDate()).append("\n");
        sb.append("Payment Terms: ").append(po.getPaymentTerms()).append("\n");
        sb.append("Status: ").append(po.getStatus()).append("\n\n");

        sb.append("Supplier:\n");
        sb.append("  Name: ").append(po.getSupplier().getCompanyName()).append("\n");
        sb.append("  Code: ").append(po.getSupplier().getSupplierCode()).append("\n\n");

        sb.append("Items:\n");
        sb.append(String.format("  %-20s %-10s %-12s %-12s\n", "Product", "Qty", "Unit Price", "Total"));
        sb.append("  --------------------------------------------------\n");

        for (PurchaseOrderItem item : po.getItems()) {
            sb.append(String.format("  %-20s %-10d %-12s %-12s\n",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()));
        }

        sb.append("  --------------------------------------------------\n");
        sb.append("Subtotal: ").append(po.getSubtotal()).append("\n");
        sb.append("Total Amount: ").append(po.getTotalAmount()).append("\n");
        sb.append("====================================================\n");

        return sb.toString();
    }

    private void copyItemsFromQuotation(PurchaseOrder po, Quotation quotation) {
        for (QuotationItem qItem : quotation.getItems()) {
            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(qItem.getProduct())
                    .quantity(qItem.getQuantity())
                    .unitPrice(qItem.getUnitPrice())
                    .totalPrice(qItem.getTotalPrice())
                    .build();
            po.addItem(poItem);
        }
    }

    private void processAndAddCustomItems(PurchaseOrder po, List<PurchaseOrderItemRequest> itemRequests) {
        for (PurchaseOrderItemRequest itemReq : itemRequests) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }

            if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Unit price cannot be negative");
            }

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();

            po.addItem(item);
        }
    }

    private void recalculateTotals(PurchaseOrder po) {
        BigDecimal total = BigDecimal.ZERO;
        if (po.getItems() != null) {
            for (PurchaseOrderItem item : po.getItems()) {
                total = total.add(item.getTotalPrice());
            }
        }
        po.setSubtotal(total);
        po.setTotalAmount(total);
    }

    private String generateUniquePoNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PO-" + timestamp + "-" + randomHex;
    }
}
