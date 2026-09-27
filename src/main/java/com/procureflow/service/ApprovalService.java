package com.procureflow.service;

import com.procureflow.dto.approval.ApprovalDecisionRequest;
import com.procureflow.dto.approval.ApprovalResponse;
import com.procureflow.entity.Approval;
import com.procureflow.entity.PurchaseRequest;
import com.procureflow.entity.User;
import com.procureflow.enums.ApprovalStatus;
import com.procureflow.enums.RequestStatus;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.ApprovalMapper;
import com.procureflow.repository.ApprovalRepository;
import com.procureflow.repository.PurchaseRequestRepository;
import com.procureflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final ApprovalMapper approvalMapper;

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getPendingApprovals() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING)
                .stream()
                .map(approvalMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse getById(Long id) {
        Approval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", id));
        return approvalMapper.toResponse(approval);
    }

    @Transactional
    public ApprovalResponse approve(Long approvalId, ApprovalDecisionRequest decisionRequest, String approverEmail) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Approval has already been processed");
        }

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", approverEmail));

        PurchaseRequest pr = approval.getPurchaseRequest();
        if (approver.getId().equals(pr.getRequestedBy().getId())) {
            throw new BadRequestException("Approver cannot be the creator of the purchase request");
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setApprover(approver);
        approval.setApprovedAt(LocalDateTime.now());
        if (decisionRequest != null) {
            approval.setComments(decisionRequest.getComments());
        }

        pr.setStatus(RequestStatus.APPROVED);
        purchaseRequestRepository.save(pr);

        Approval saved = approvalRepository.save(approval);
        log.info("Approved purchase request id={} number='{}' by approver='{}'", pr.getId(), pr.getRequestNumber(), approverEmail);

        return approvalMapper.toResponse(saved);
    }

    @Transactional
    public ApprovalResponse reject(Long approvalId, ApprovalDecisionRequest decisionRequest, String approverEmail) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", "id", approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Approval has already been processed");
        }

        if (decisionRequest == null || decisionRequest.getComments() == null || decisionRequest.getComments().isBlank()) {
            throw new BadRequestException("Rejection reason is required");
        }

        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", approverEmail));

        PurchaseRequest pr = approval.getPurchaseRequest();
        if (approver.getId().equals(pr.getRequestedBy().getId())) {
            throw new BadRequestException("Approver cannot be the creator of the purchase request");
        }

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setApprover(approver);
        approval.setRejectedAt(LocalDateTime.now());
        approval.setComments(decisionRequest.getComments());

        pr.setStatus(RequestStatus.REJECTED);
        purchaseRequestRepository.save(pr);

        Approval saved = approvalRepository.save(approval);
        log.info("Rejected purchase request id={} number='{}' by approver='{}' reason='{}'", pr.getId(), pr.getRequestNumber(), approverEmail, decisionRequest.getComments());

        return approvalMapper.toResponse(saved);
    }
}
