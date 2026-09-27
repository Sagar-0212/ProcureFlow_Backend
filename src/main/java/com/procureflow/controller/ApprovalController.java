package com.procureflow.controller;

import com.procureflow.dto.approval.ApprovalDecisionRequest;
import com.procureflow.dto.approval.ApprovalResponse;
import com.procureflow.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        return ResponseEntity.ok(approvalService.getPendingApprovals());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApprovalResponse> getApprovalById(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalDecisionRequest decisionRequest,
            Authentication authentication) {
        String approverEmail = authentication.getName();
        return ResponseEntity.ok(approvalService.approve(id, decisionRequest, approverEmail));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApprovalResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest decisionRequest,
            Authentication authentication) {
        String approverEmail = authentication.getName();
        return ResponseEntity.ok(approvalService.reject(id, decisionRequest, approverEmail));
    }
}
