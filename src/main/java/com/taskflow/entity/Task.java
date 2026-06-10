package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ============================================================
 * TASK ENTITY — A unit of work inside a Project
 * ============================================================
 *
 * DATABASE TABLE: "tasks"
 *
 * RELATIONSHIPS IN THIS ENTITY:
 *
 * @ManyToOne project  — Many tasks belong to 1 project (project_id FK in tasks)
 * @ManyToOne assignee — Many tasks can be assigned to 1 user (nullable — unassigned)
 * @ManyToOne reporter — Many tasks were reported by 1 user (who created this task)
 *
 * NOTICE: Task has @ManyToOne for all 3 relationships.
 *   This is the "OWNING" side — the tasks table physically has these FK columns:
 *     tasks.project_id  → references projects.id
 *     tasks.assignee_id → references users.id  (NULLABLE — can be unassigned)
 *     tasks.reporter_id → references users.id
 *
 * REAL WORLD EXAMPLE:
 *   Task: "Fix login bug"
 *     project  = "TaskFlow App" (must have a project)
 *     assignee = "Alice" (the developer fixing it — can be empty if not yet assigned)
 *     reporter = "Bob" (the PM who found and logged the bug — must have a reporter)
 *
 * WHY TWO SEPARATE USER REFERENCES (assignee vs reporter)?
 *   In tools like Jira, JIRA-style:
 *     Reporter  = who logged the issue (accountability, notifications)
 *     Assignee  = who is working on it (current responsibility)
 *   These are often different people. The reporter doesn't change; assignee can.
 *
 * HOW @ManyToOne IS DIFFERENT FROM @ManyToMany:
 *   @ManyToOne:  tasks table has a simple FK column (project_id INT)
 *   @ManyToMany: requires a separate join table (project_members)
 * ============================================================
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Basic Fields ----

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private LocalDate dueDate;

    private Integer estimatedHours;  // Planned time to complete
    private Integer actualHours;     // Actual time taken (filled when done)

    // ---- RELATIONSHIP 1: @ManyToOne to Project ----
    //
    // This is the OWNING SIDE of the Project <-> Task relationship.
    // The tasks table has a "project_id" column.
    //
    // When Project has @OneToMany(mappedBy = "project"),
    // it's saying: "Task.project is the real owner. Look there."
    //
    // nullable = false → A task MUST belong to a project. No orphan tasks.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    private Project project;

    // ---- RELATIONSHIP 2: @ManyToOne to User (Assignee) ----
    //
    // nullable = true (default) → A task may be UNASSIGNED.
    // The tasks table has an "assignee_id" column that can be NULL.
    //
    // Real world: Newly created tasks often have no assignee.
    //             PM assigns them in the sprint planning meeting.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")  // No nullable = false → allows NULL
    @ToString.Exclude
    private User assignee;

    // ---- RELATIONSHIP 3: @ManyToOne to User (Reporter) ----
    //
    // The user who CREATED this task (logged the bug / wrote the story).
    // Set automatically in TaskService based on the current logged-in user.
    // CANNOT be null — every task must have a reporter.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @ToString.Exclude
    private User reporter;

    // ---- Audit Fields ----

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
