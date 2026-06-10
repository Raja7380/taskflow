package com.taskflow.entity;

/**
 * TASK STATUS — Tracks where a task is in its workflow.
 *
 * This is a Kanban-style workflow:
 *   TODO → IN_PROGRESS → IN_REVIEW → DONE
 *                         ↓
 *                      CANCELLED (at any point)
 *
 * INTERVIEW Q: Why model status as an enum instead of a boolean "done" field?
 * A: A boolean can't represent intermediate states like IN_REVIEW.
 *    Enums scale better — adding a new state is one line, not a schema redesign.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    CANCELLED
}
