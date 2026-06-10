package com.taskflow.entity;

/**
 * PROJECT STATUS — Tracks the lifecycle of a project.
 *
 * PLANNING  → Project is defined, work hasn't started
 * ACTIVE    → Work is actively in progress
 * ON_HOLD   → Temporarily paused
 * COMPLETED → All work done, project closed
 * ARCHIVED  → Read-only, historical record
 *
 * Think of it like a Kanban column but for the whole project,
 * not individual tasks.
 */
public enum ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    ARCHIVED
}
