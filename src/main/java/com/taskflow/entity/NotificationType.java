package com.taskflow.entity;

public enum NotificationType {
    TASK_CREATED,         // A new task was created in your project
    TASK_ASSIGNED,        // You were assigned to a task
    TASK_STATUS_CHANGED,  // A task you reported changed status
    PROJECT_CREATED,      // You created a new project (confirmation)
    TASK_DUE_SOON,        // A task assigned to you is due within 24 hours
    TASK_OVERDUE          // A task assigned to you is past its due date
}
