package com.procureflow.controller;

import com.procureflow.dto.audit.AuditLogResponse;
import com.procureflow.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getById(id));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getByEntity(entityType, entityId));
    }

    @RequestMapping(value = {"", "/**"}, method = {RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> auditLogsNotModifiable() {
        return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
