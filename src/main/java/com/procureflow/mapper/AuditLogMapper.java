package com.procureflow.mapper;

import com.procureflow.dto.audit.AuditLogResponse;
import com.procureflow.entity.AuditLog;
import com.procureflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog log) {
        if (log == null) {
            return null;
        }

        User user = log.getUser();
        String userName = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "System";
        String userEmail = user != null ? user.getEmail() : null;

        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(user != null ? user.getId() : null)
                .userName(userName)
                .userEmail(userEmail)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .timestamp(log.getTimestamp())
                .build();
    }
}
