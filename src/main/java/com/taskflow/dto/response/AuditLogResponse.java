package com.taskflow.dto.response;

import com.taskflow.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private String action;
    private String entityType;
    private Long userId;
    private String userEmail;
    private Long durationMs;
    private boolean success;
    private String errorMessage;
    private LocalDateTime timestamp;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .durationMs(log.getDurationMs())
                .success(log.isSuccess())
                .errorMessage(log.getErrorMessage())
                .timestamp(log.getTimestamp())
                .build();
    }
}
