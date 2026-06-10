package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * NOTIFICATION ENTITY — In-app notifications for users.
 *
 * Real examples of this pattern:
 *   - GitHub: "Raja commented on your PR"
 *   - Jira: "Alice assigned TASK-123 to you"
 *   - Slack: "You were mentioned in #general"
 *   - LinkedIn: "Your post got 50 new likes"
 *
 * DESIGN: We store userId (not @ManyToOne User) to avoid cascade issues.
 * The notification belongs to a user but we don't need to load the full User
 * object when working with notifications. Just the ID is enough to query
 * "give me all notifications for user 5".
 *
 * `read` field: false = new/unread, true = user has seen it.
 * Used to show the red notification badge (count of unread notifications).
 */
@Entity
@Table(name = "notifications",
       indexes = {
           @Index(name = "idx_notification_user_id", columnList = "user_id"),
           @Index(name = "idx_notification_read", columnList = "user_id, read")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;            // who receives this notification

    @Column(nullable = false, length = 500)
    private String message;         // "New task 'Fix login bug' was created in 'Mobile App'"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;  // TASK_CREATED, TASK_ASSIGNED, etc.

    @Column(name = "related_entity_id")
    private Long relatedEntityId;   // Optional: the task ID or project ID this relates to

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType; // "Task" or "Project"

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;   // false = unread, true = user has seen it

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
