package com.procureflow.service;

import com.procureflow.dto.purchaserequest.PurchaseRequestItemRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestResponse;
import com.procureflow.entity.Department;
import com.procureflow.entity.Product;
import com.procureflow.entity.PurchaseRequest;
import com.procureflow.entity.PurchaseRequestItem;
import com.procureflow.entity.User;
import com.procureflow.enums.RequestStatus;
import com.procureflow.enums.RoleName;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.PurchaseRequestMapper;
import com.procureflow.entity.Approval;
import com.procureflow.entity.ApprovalRule;
import com.procureflow.enums.ApprovalStatus;
import com.procureflow.repository.ApprovalRepository;
import com.procureflow.repository.ApprovalRuleRepository;
import com.procureflow.repository.DepartmentRepository;
import com.procureflow.repository.ProductRepository;
import com.procureflow.repository.PurchaseRequestRepository;
import com.procureflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApprovalRepository approvalRepository;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public PurchaseRequestResponse create(PurchaseRequestRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Purchase request must contain at least one item");
        }

        String requestNumber = generateUniqueRequestNumber();

        PurchaseRequest pr = PurchaseRequest.builder()
                .requestNumber(requestNumber)
                .requestedBy(user)
                .department(department)
                .title(request.getTitle())
                .reason(request.getReason())
                .status(RequestStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal grandTotal = processAndAddItems(pr, request.getItems());
        pr.setTotalAmount(grandTotal);

        PurchaseRequest savedPr = purchaseRequestRepository.save(pr);
        log.info("Created purchase request id={} number='{}' by user='{}'", savedPr.getId(), savedPr.getRequestNumber(), currentUserEmail);

        auditLogService.logAction(user, "CREATE_PR", "PurchaseRequest", savedPr.getId(), "Created purchase request " + savedPr.getRequestNumber());

        return purchaseRequestMapper.toResponse(savedPr);
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse getById(Long id, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        PurchaseRequest pr = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        boolean isManagerOrAdmin = isManagerOrAdminOrProcurement(user);
        boolean isOwner = pr.getRequestedBy().getId().equals(user.getId());

        if (!isOwner && !isManagerOrAdmin) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view this purchase request");
        }

        return purchaseRequestMapper.toResponse(pr);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getAll(String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        List<PurchaseRequest> requests;
        if (isManagerOrAdminOrProcurement(user)) {
            requests = purchaseRequestRepository.findAll();
        } else {
            requests = purchaseRequestRepository.findByRequestedById(user.getId());
        }

        return requests.stream()
                .map(purchaseRequestMapper::toResponse)
                .toList();
    }

    @Transactional
    public PurchaseRequestResponse update(Long id, PurchaseRequestRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        PurchaseRequest pr = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        if (!pr.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: Only the creator can edit this purchase request");
        }

        if (pr.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Cannot modify purchase request with status: " + pr.getStatus());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Purchase request must contain at least one item");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        pr.setTitle(request.getTitle());
        pr.setReason(request.getReason());
        pr.setDepartment(department);

        pr.clearItems();
        BigDecimal grandTotal = processAndAddItems(pr, request.getItems());
        pr.setTotalAmount(grandTotal);

        PurchaseRequest updatedPr = purchaseRequestRepository.save(pr);
        log.info("Updated purchase request id={} status='{}'", updatedPr.getId(), updatedPr.getStatus());

        return purchaseRequestMapper.toResponse(updatedPr);
    }

    @Transactional
    public void delete(Long id, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        PurchaseRequest pr = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        if (!pr.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: Only the creator can delete this purchase request");
        }

        if (pr.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Cannot delete purchase request with status: " + pr.getStatus());
        }

        purchaseRequestRepository.delete(pr);
        log.info("Deleted DRAFT purchase request id={}", id);
    }

    @Transactional
    public PurchaseRequestResponse submit(Long id, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        PurchaseRequest pr = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        if (!pr.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: Only the creator can submit this purchase request");
        }

        if (pr.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase requests can be submitted");
        }

        if (pr.getItems() == null || pr.getItems().isEmpty()) {
            throw new BadRequestException("Cannot submit an empty purchase request");
        }

        // Determine applicable approval rule from totalAmount
        ApprovalRule matchingRule = approvalRuleRepository.findMatchingRule(pr.getTotalAmount())
                .orElseThrow(() -> new BadRequestException("No active approval rule found for purchase request amount: " + pr.getTotalAmount()));

        pr.setStatus(RequestStatus.PENDING_APPROVAL);
        PurchaseRequest submittedPr = purchaseRequestRepository.save(pr);

        Approval approval = Approval.builder()
                .purchaseRequest(submittedPr)
                .status(ApprovalStatus.PENDING)
                .build();
        approvalRepository.save(approval);

        log.info("Submitted purchase request id={} number='{}' status='{}' matching rule id={}", submittedPr.getId(), submittedPr.getRequestNumber(), submittedPr.getStatus(), matchingRule.getId());

        auditLogService.logAction(user, "SUBMIT_PR", "PurchaseRequest", submittedPr.getId(), "Submitted purchase request " + submittedPr.getRequestNumber());

        return purchaseRequestMapper.toResponse(submittedPr);
    }

    private BigDecimal processAndAddItems(PurchaseRequest pr, List<PurchaseRequestItemRequest> itemRequests) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseRequestItemRequest itemReq : itemRequests) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }

            if (itemReq.getEstimatedUnitPrice() == null || itemReq.getEstimatedUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Estimated unit price must not be negative");
            }

            BigDecimal itemTotal = itemReq.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            PurchaseRequestItem item = PurchaseRequestItem.builder()
                    .purchaseRequest(pr)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .estimatedUnitPrice(itemReq.getEstimatedUnitPrice())
                    .estimatedTotal(itemTotal)
                    .build();

            pr.addItem(item);
            total = total.add(itemTotal);
        }
        return total;
    }

    private String generateUniqueRequestNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PR-" + timestamp + "-" + randomHex;
    }

    private boolean isManagerOrAdminOrProcurement(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        RoleName roleName = user.getRole().getName();
        return roleName == RoleName.ADMIN || roleName == RoleName.MANAGER || roleName == RoleName.PROCUREMENT_OFFICER;
    }
}
