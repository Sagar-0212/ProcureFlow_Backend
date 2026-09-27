package com.procureflow.service;

import com.procureflow.dto.audit.AuditLogResponse;
import com.procureflow.entity.AuditLog;
import com.procureflow.entity.User;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.AuditLogMapper;
import com.procureflow.repository.AuditLogRepository;
import com.procureflow.repository.UserRepository;
import com.procureflow.security.ProcureFlowUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public AuditLog logAction(User user, String action, String entityType, Long entityId, String description) {
        User actingUser = user != null ? user : getCurrentUser();
        AuditLog auditLog = AuditLog.builder()
                .user(actingUser)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("AuditLog recorded: action='{}' entityType='{}' entityId={} by user='{}'",
                action, entityType, entityId, actingUser != null ? actingUser.getEmail() : "System");
        return saved;
    }

    @Transactional
    public AuditLog logAction(String action, String entityType, Long entityId, String description) {
        return logAction(null, action, entityType, entityId, description);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return auditLogMapper.toResponse(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
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
}
