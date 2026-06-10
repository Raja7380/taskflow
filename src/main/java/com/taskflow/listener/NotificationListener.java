package com.taskflow.listener;

import com.taskflow.entity.NotificationType;
import com.taskflow.event.ProjectCreatedEvent;
import com.taskflow.event.TaskAssignedEvent;
import com.taskflow.event.TaskCreatedEvent;
import com.taskflow.event.TaskStatusChangedEvent;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * NOTIFICATION LISTENER — Reacts to domain events and creates notifications.
 * ============================================================
 *
 * WHY THE PUBLISHER-SUBSCRIBER PATTERN (Spring Events) EXISTS:
 *
 * Problem without events:
 *   When a task is created, TaskService needs to:
 *   1. Save the task (its actual job)
 *   2. Create a notification for the project owner
 *   3. Maybe send an email (future)
 *   4. Maybe update analytics (future)
 *   5. Maybe trigger a Slack message (future)
 *
 *   All of this code ends up IN TaskService. TaskService is now:
 *   - Doing its job (creating tasks) AND
 *   - Doing notification logic AND
 *   - Doing email logic AND
 *   - Doing analytics logic
 *
 *   This violates Single Responsibility Principle.
 *   TaskService now depends on NotificationService, EmailService, AnalyticsService.
 *   Adding a new reaction to "task created" = editing TaskService every time.
 *   Testing TaskService requires mocking all those dependencies.
 *
 * Solution with events (what we built):
 *   TaskService's only job: save the task + publish "a task was created."
 *   NotificationListener's job: react to "task was created" → create notification.
 *   EmailListener (future): react to "task was created" → send email.
 *   AnalyticsListener (future): react to "task was created" → update stats.
 *
 *   TaskService doesn't know or care what happens after it publishes.
 *   New reactions = add a new listener class. TaskService is UNTOUCHED.
 *
 * This is the OPEN/CLOSED PRINCIPLE:
 *   "Open for extension (add new listeners) — Closed for modification (don't touch TaskService)"
 *
 * REAL WORLD: This is exactly how Stripe, GitHub, AWS work with webhooks.
 *   Stripe: "payment_succeeded" event → your listener handles it
 *   GitHub: "push" event → CI/CD listener runs tests
 *   AWS: "S3 object created" event → Lambda listener processes the file
 *
 * @EventListener — method is called when the matching event is published.
 *   Spring matches the event type to the method parameter type.
 *   TaskCreatedEvent published → handleTaskCreated(TaskCreatedEvent) is called.
 *   The matching is done by type — one listener can handle multiple event types.
 * ============================================================
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    /**
     * When a task is created → notify the PROJECT OWNER.
     *
     * Why notify the owner? They want to know what's happening in their project.
     * (In Jira: project lead gets notified when issues are created.)
     *
     * We don't notify the creator — they know, they just created it!
     */
    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        Long ownerId = event.getTask().getProject().getOwner().getId();

        // Don't notify if the creator IS the owner (they already know)
        if (ownerId.equals(event.getCreator().getId())) {
            return;
        }

        String message = String.format("New task created in '%s': \"%s\"",
                event.getTask().getProject().getName(),
                event.getTask().getTitle());

        notificationService.create(
                ownerId,
                message,
                NotificationType.TASK_CREATED,
                event.getTask().getId(),
                "Task"
        );
        log.info("[EVENT] TaskCreated → notification sent to owner {}", ownerId);
    }

    /**
     * When a task is assigned → notify the NEW ASSIGNEE.
     *
     * Why? The developer needs to know they have work to do.
     * (Jira sends an email when someone assigns a ticket to you.)
     */
    @EventListener
    public void handleTaskAssigned(TaskAssignedEvent event) {
        String message = String.format("You were assigned to task: \"%s\" in project '%s'",
                event.getTask().getTitle(),
                event.getTask().getProject().getName());

        notificationService.create(
                event.getNewAssignee().getId(),
                message,
                NotificationType.TASK_ASSIGNED,
                event.getTask().getId(),
                "Task"
        );
        log.info("[EVENT] TaskAssigned → notification sent to assignee {}", event.getNewAssignee().getId());
    }

    /**
     * When task status changes → notify the REPORTER.
     *
     * Why? The reporter (often the PM who logged the bug) wants to know progress.
     * Don't notify if the reporter IS the one who made the change — they know.
     */
    @EventListener
    public void handleTaskStatusChanged(TaskStatusChangedEvent event) {
        Long reporterId = event.getTask().getReporter().getId();

        // Don't notify if the reporter changed it themselves
        if (reporterId.equals(event.getChangedBy().getId())) {
            return;
        }

        String message = String.format("Task \"%s\" moved from %s → %s",
                event.getTask().getTitle(),
                event.getPreviousStatus(),
                event.getNewStatus());

        notificationService.create(
                reporterId,
                message,
                NotificationType.TASK_STATUS_CHANGED,
                event.getTask().getId(),
                "Task"
        );
        log.info("[EVENT] TaskStatusChanged → notification sent to reporter {}", reporterId);
    }

    /**
     * When a project is created → log it (no notification needed for creator).
     * Could be used to notify admins, update analytics, etc.
     */
    @EventListener
    public void handleProjectCreated(ProjectCreatedEvent event) {
        log.info("[EVENT] ProjectCreated: '{}' by user {}",
                event.getProject().getName(), event.getCreator().getEmail());
        // Future: send welcome email, notify admins, update dashboard metrics
    }
}
