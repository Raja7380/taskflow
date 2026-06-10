package com.taskflow.entity;

/**
 * PRIORITY ENUM — Shared by both Project and Task entities.
 *
 * WHAT: Defines urgency levels for projects and tasks.
 * WHY:  Using an enum instead of a String prevents typos and limits values.
 *       String "HIHG" would silently save. Priority.HIHG won't compile.
 *
 * INTERVIEW Q: Why use enum over String for status/priority fields?
 * A: 1. Type-safe — compiler catches invalid values
 *    2. Limited set — only these values exist, no unexpected data in DB
 *    3. Can add behavior — e.g., priority.isUrgent() method
 *    4. IDE autocomplete — no typos
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
