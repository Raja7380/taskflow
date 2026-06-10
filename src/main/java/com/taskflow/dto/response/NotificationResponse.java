package com.taskflow.dto.response;

import com.taskflow.entity.Notification;
import com.taskflow.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String message;
    private NotificationType type;
    private Long relatedEntityId;
    private String relatedEntityType;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
