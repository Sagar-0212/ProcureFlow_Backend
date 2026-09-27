package com.procureflow.service;

import com.procureflow.dto.receipt.CreateGoodsReceiptRequest;
import com.procureflow.dto.receipt.GoodsReceiptItemRequest;
import com.procureflow.dto.receipt.GoodsReceiptResponse;
import com.procureflow.entity.*;
import com.procureflow.enums.GoodsReceiptStatus;
import com.procureflow.enums.POStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.GoodsReceiptMapper;
import com.procureflow.repository.GoodsReceiptItemRepository;
import com.procureflow.repository.GoodsReceiptRepository;
import com.procureflow.repository.PurchaseOrderRepository;
import com.procureflow.repository.UserRepository;
import com.procureflow.security.ProcureFlowUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final GoodsReceiptMapper goodsReceiptMapper;

    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    @Transactional
    public GoodsReceiptResponse create(CreateGoodsReceiptRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", request.getPurchaseOrderId()));

        if (po.getStatus() != POStatus.SENT_TO_SUPPLIER && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Goods receipt can only be created for Purchase Orders in SENT_TO_SUPPLIER or PARTIALLY_RECEIVED status");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Goods receipt must contain at least one item");
        }

        User currentUser = getCurrentUser();
        String receiptNumber = generateUniqueReceiptNumber();

        GoodsReceipt receipt = GoodsReceipt.builder()
                .receiptNumber(receiptNumber)
                .purchaseOrder(po)
                .receivedDate(request.getReceivedDate())
                .receivedBy(currentUser)
                .notes(request.getNotes())
                .status(GoodsReceiptStatus.RECEIVED)
                .build();

        processAndAddItems(receipt, po, request.getItems());

        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);
        log.info("Created GoodsReceipt id={} receiptNumber='{}' for PO id={}", savedReceipt.getId(), savedReceipt.getReceiptNumber(), po.getId());

        updatePurchaseOrderStatus(po);

        // Process Inventory Update for Accepted Quantities
        inventoryService.processGoodsReceipt(savedReceipt);

        auditLogService.logAction("RECEIVE_GOODS", "GoodsReceipt", savedReceipt.getId(), "Received Goods Receipt #" + savedReceipt.getReceiptNumber());

        return goodsReceiptMapper.toResponse(savedReceipt);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(Long id) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceipt", "id", id));
        return goodsReceiptMapper.toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getAll() {
        return goodsReceiptRepository.findAll()
                .stream()
                .map(goodsReceiptMapper::toResponse)
                .toList();
    }

    @Transactional
    public GoodsReceiptResponse receive(Long id) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceipt", "id", id));

        if (receipt.getStatus() == GoodsReceiptStatus.RECEIVED) {
            return goodsReceiptMapper.toResponse(receipt);
        }

        if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED) {
            throw new BadRequestException("Cannot receive a CANCELLED goods receipt");
        }

        PurchaseOrder po = receipt.getPurchaseOrder();
        if (po.getStatus() != POStatus.SENT_TO_SUPPLIER && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Purchase order is not in a valid status for receiving goods");
        }

        // Validate items remaining quantities
        for (GoodsReceiptItem item : receipt.getItems()) {
            PurchaseOrderItem poItem = item.getPurchaseOrderItem();
            Integer alreadyReceived = goodsReceiptItemRepository.sumReceivedQuantityByPurchaseOrderItemId(poItem.getId());
            Integer remaining = poItem.getQuantity() - alreadyReceived;
            if (item.getReceivedQuantity() > remaining) {
                throw new BadRequestException("Received quantity (" + item.getReceivedQuantity() +
                        ") exceeds remaining quantity (" + remaining + ") for PO item id=" + poItem.getId());
            }
        }

        receipt.setStatus(GoodsReceiptStatus.RECEIVED);
        GoodsReceipt updatedReceipt = goodsReceiptRepository.save(receipt);
        log.info("GoodsReceipt id={} status updated to RECEIVED", updatedReceipt.getId());

        updatePurchaseOrderStatus(po);

        inventoryService.processGoodsReceipt(updatedReceipt);

        auditLogService.logAction("RECEIVE_GOODS", "GoodsReceipt", updatedReceipt.getId(), "Received Goods Receipt #" + updatedReceipt.getReceiptNumber());

        return goodsReceiptMapper.toResponse(updatedReceipt);
    }

    @Transactional
    public GoodsReceiptResponse cancel(Long id) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceipt", "id", id));

        if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED) {
            return goodsReceiptMapper.toResponse(receipt);
        }

        receipt.setStatus(GoodsReceiptStatus.CANCELLED);
        GoodsReceipt cancelledReceipt = goodsReceiptRepository.save(receipt);
        log.info("Cancelled GoodsReceipt id={}", cancelledReceipt.getId());

        updatePurchaseOrderStatus(receipt.getPurchaseOrder());

        auditLogService.logAction("CANCEL_GOODS_RECEIPT", "GoodsReceipt", cancelledReceipt.getId(), "Cancelled Goods Receipt #" + cancelledReceipt.getReceiptNumber());

        return goodsReceiptMapper.toResponse(cancelledReceipt);
    }

    private void processAndAddItems(GoodsReceipt receipt, PurchaseOrder po, List<GoodsReceiptItemRequest> itemRequests) {
        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(java.util.stream.Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        for (GoodsReceiptItemRequest itemReq : itemRequests) {
            PurchaseOrderItem poItem = poItemMap.get(itemReq.getPurchaseOrderItemId());
            if (poItem == null) {
                throw new BadRequestException("PO item id=" + itemReq.getPurchaseOrderItemId() + " does not belong to Purchase Order id=" + po.getId());
            }

            if (itemReq.getReceivedQuantity() == null || itemReq.getReceivedQuantity() <= 0) {
                throw new BadRequestException("Received quantity must be greater than 0");
            }

            int accepted = itemReq.getAcceptedQuantity() != null ? itemReq.getAcceptedQuantity() : 0;
            int rejected = itemReq.getRejectedQuantity() != null ? itemReq.getRejectedQuantity() : 0;

            if (accepted + rejected != itemReq.getReceivedQuantity()) {
                throw new BadRequestException("Accepted quantity (" + accepted + ") plus rejected quantity (" + rejected +
                        ") must equal received quantity (" + itemReq.getReceivedQuantity() + ")");
            }

            Integer alreadyReceived = goodsReceiptItemRepository.sumReceivedQuantityByPurchaseOrderItemId(poItem.getId());
            Integer remaining = poItem.getQuantity() - alreadyReceived;

            if (itemReq.getReceivedQuantity() > remaining) {
                throw new BadRequestException("Received quantity (" + itemReq.getReceivedQuantity() +
                        ") exceeds remaining quantity (" + remaining + ") for PO item id=" + poItem.getId());
            }

            GoodsReceiptItem receiptItem = GoodsReceiptItem.builder()
                    .goodsReceipt(receipt)
                    .purchaseOrderItem(poItem)
                    .receivedQuantity(itemReq.getReceivedQuantity())
                    .acceptedQuantity(accepted)
                    .rejectedQuantity(rejected)
                    .remarks(itemReq.getRemarks())
                    .build();

            receipt.addItem(receiptItem);
        }
    }

    private void updatePurchaseOrderStatus(PurchaseOrder po) {
        int totalOrdered = 0;
        int totalReceived = 0;

        for (PurchaseOrderItem poItem : po.getItems()) {
            totalOrdered += poItem.getQuantity();
            Integer itemReceived = goodsReceiptItemRepository.sumReceivedQuantityByPurchaseOrderItemId(poItem.getId());
            totalReceived += itemReceived;
        }

        if (totalReceived == 0) {
            po.setStatus(POStatus.SENT_TO_SUPPLIER);
        } else if (totalReceived >= totalOrdered) {
            po.setStatus(POStatus.FULLY_RECEIVED);
        } else {
            po.setStatus(POStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrderRepository.save(po);
        log.info("Updated PurchaseOrder id={} status to '{}' (received {}/{} items)", po.getId(), po.getStatus(), totalReceived, totalOrdered);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ProcureFlowUserDetails details) {
            return details.getUser();
        }
        if (auth != null && auth.getName() != null) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private String generateUniqueReceiptNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "GR-" + timestamp + "-" + randomHex;
    }
}
