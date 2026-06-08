package com.taskflow.entity;

/**
 * ROLE ENUM — Defines user permission levels.
 *
 * WHY an enum instead of a String?
 *   1. Type-safe — compiler catches typos (Role.ADMIN vs "ADMN")
 *   2. Limited values — only these 3 roles exist, no others possible
 *   3. Can add methods — e.g., role.canManageUsers()
 *
 * HOW it maps to the database:
 *   @Enumerated(EnumType.STRING) in the User entity stores "ADMIN", "MANAGER", "USER"
 *   as text in the DB column. NEVER use EnumType.ORDINAL (stores 0,1,2 — breaks if you
 *   reorder the enum values!).
 *
 * INTERVIEW Q: Why use EnumType.STRING over EnumType.ORDINAL?
 * A: ORDINAL stores position (0, 1, 2). If you add a new role between existing ones
 *    or reorder, all existing data becomes wrong. STRING stores the name ("ADMIN"),
 *    which is safe regardless of ordering.
 */
public enum Role {
    USER,       // Regular user — can manage own tasks
    MANAGER,    // Team lead — can manage team tasks + assign work
    ADMIN       // Admin — full access to everything
}
