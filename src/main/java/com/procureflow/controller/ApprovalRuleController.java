package com.procureflow.controller;

import com.procureflow.dto.approval.ApprovalRuleRequest;
import com.procureflow.dto.approval.ApprovalRuleResponse;
import com.procureflow.service.ApprovalRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-rules")
@RequiredArgsConstructor
public class ApprovalRuleController {

    private final ApprovalRuleService approvalRuleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalRuleResponse> createApprovalRule(@Valid @RequestBody ApprovalRuleRequest request) {
        ApprovalRuleResponse response = approvalRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApprovalRuleResponse>> getAllApprovalRules() {
        return ResponseEntity.ok(approvalRuleService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRuleResponse> getApprovalRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(approvalRuleService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalRuleResponse> updateApprovalRule(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRuleRequest request) {
        return ResponseEntity.ok(approvalRuleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteApprovalRule(@PathVariable Long id) {
        approvalRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
