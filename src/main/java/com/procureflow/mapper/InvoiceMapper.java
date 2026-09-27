package com.procureflow.mapper;

import com.procureflow.dto.invoice.InvoiceItemResponse;
import com.procureflow.dto.invoice.InvoiceResponse;
import com.procureflow.entity.Invoice;
import com.procureflow.entity.InvoiceItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        List<InvoiceItemResponse> itemResponses = invoice.getItems() != null ?
                invoice.getItems().stream().map(this::toItemResponse).toList() : List.of();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .supplierInvoiceNumber(invoice.getSupplierInvoiceNumber())
                .purchaseOrderId(invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getId() : null)
                .purchaseOrderNumber(invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getPoNumber() : null)
                .supplierId(invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null ?
                        invoice.getPurchaseOrder().getSupplier().getId() : null)
                .supplierName(invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null ?
                        invoice.getPurchaseOrder().getSupplier().getCompanyName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .mismatchReason(invoice.getMismatchReason())
                .notes(invoice.getNotes())
                .items(itemResponses)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    public InvoiceItemResponse toItemResponse(InvoiceItem item) {
        if (item == null) {
            return null;
        }

        return InvoiceItemResponse.builder()
                .id(item.getId())
                .purchaseOrderItemId(item.getPurchaseOrderItem() != null ? item.getPurchaseOrderItem().getId() : null)
                .productId(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getId() : null)
                .productName(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getName() : null)
                .productSku(item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getProduct() != null ?
                        item.getPurchaseOrderItem().getProduct().getSku() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
