package com.taskflow.exception;

import com.taskflow.entity.TaskStatus;

/**
 * Thrown when a task status change is not allowed by the state machine.
 * Example: trying to move a DONE task back to TODO.
 * Handled by GlobalExceptionHandler → returns 400 Bad Request.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TaskStatus from, TaskStatus to) {
        super("Invalid task status transition: " + from + " → " + to +
              ". Check the allowed transitions in TaskService.");
    }
}
