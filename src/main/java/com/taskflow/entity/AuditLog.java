package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AUDIT LOG ENTITY — Records every significant action in the system.
 *
 * WHY AUDIT LOGS EXIST IN REAL COMPANIES:
 *   - Compliance: GDPR, SOC2, ISO27001 all REQUIRE audit trails
 *   - Security: "Who deleted that record?" "Who changed that price?"
 *   - Debugging: "What exactly happened before the crash?"
 *   - Legal: Banks must prove who authorized a transaction
 *
 * Examples from real companies:
 *   - GitHub: full audit log of every action in an organization
 *   - Jira: complete history of every field change on every issue
 *   - AWS: CloudTrail records every API call to your account
 *   - Banks: every login, every transaction, every config change — logged
 *
 * DESIGN DECISION — we store userId AND userEmail separately:
 *   If we only stored userId and the user is later deleted,
 *   we'd have a userId pointing to nothing — orphaned audit record.
 *   Storing the email as a snapshot means the audit log stays readable
 *   even if the user account no longer exists.
 *   This is called "denormalization for auditability."
 */
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_user_id", columnList = "user_id"),
           @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
           @Index(name = "idx_audit_entity_type", columnList = "entity_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;         // e.g., "CREATE_PROJECT", "UPDATE_TASK", "DELETE_TASK"

    @Column(name = "entity_type", length = 50)
    private String entityType;     // e.g., "Project", "Task"

    @Column(name = "user_id")
    private Long userId;           // who performed the action

    @Column(name = "user_email", length = 150)
    private String userEmail;      // snapshot of email at time of action

    @Column(name = "duration_ms")
    private Long durationMs;       // how long the operation took (for performance monitoring)

    @Column(nullable = false)
    private boolean success;       // did it succeed or throw an exception?

    @Column(name = "error_message", length = 500)
    private String errorMessage;   // if failed, what was the error?

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
