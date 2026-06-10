package com.taskflow.service;

import com.taskflow.dto.response.NotificationResponse;
import com.taskflow.entity.Notification;
import com.taskflow.entity.NotificationType;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Called by NotificationListener to persist a notification. */
    public void create(Long userId, String message, NotificationType type,
                       Long relatedEntityId, String relatedEntityType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
        notificationRepository.save(notification);
    }

    /** Get all notifications for the current user, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::fromEntity).collect(Collectors.toList());
    }

    /** Count unread notifications — used for the badge number in the UI. */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /** Mark a single notification as read. */
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("This notification does not belong to you");
        }

        notification.setRead(true);
        return NotificationResponse.fromEntity(notification);
        // No save() needed — dirty checking commits the read=true change
    }

    /** Mark ALL notifications as read for a user (bulk update). */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }
}
