package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ============================================================
 * PROJECT ENTITY — The central piece of Session 2
 * ============================================================
 *
 * DATABASE TABLE: "projects"
 *
 * JPA RELATIONSHIPS IN THIS ENTITY — MASTER THESE FOR INTERVIEWS:
 *
 * ┌──────────────────────────────────────────────────────┐
 * │  Relationship      │  Real meaning                  │
 * ├──────────────────────────────────────────────────────┤
 * │  @ManyToOne owner  │  Many projects → 1 user owns  │
 * │  @ManyToMany mbrs  │  Many projects ↔ many users   │
 * │  @OneToMany tasks  │  1 project → many tasks        │
 * └──────────────────────────────────────────────────────┘
 *
 * @ManyToOne — "Many projects belong to one owner"
 *   SQL result: Column "owner_id" in projects table
 *   SELECT * FROM projects WHERE owner_id = 5;
 *
 * @ManyToMany — "Many projects can have many members"
 *   SQL result: A JOIN TABLE called "project_members"
 *   project_members: | project_id | user_id |
 *                    |     1      |    5    |
 *                    |     1      |    7    |
 *                    |     2      |    5    |
 *   This is a separate table — neither "projects" nor "users" stores this.
 *
 * @OneToMany — "One project has many tasks"
 *   SQL result: Column "project_id" in tasks table (foreign key)
 *   The tasks table says "I belong to project X", not the other way.
 *   "mappedBy = project" means: "the 'project' field in Task class owns this relationship"
 *
 * FETCH TYPES — Critical for performance:
 *   LAZY  = "Don't load until I ask for it" (SELECT runs only when you call getTasks())
 *   EAGER = "Load immediately with every Project query" (default for @OneToOne, @ManyToOne)
 *   Rule: ALWAYS use LAZY for collections (@OneToMany, @ManyToMany) — prevents performance disasters.
 *   Example of why EAGER kills performance:
 *     getProject(1) → automatically loads 100 members → each member loads their projects → INFINITE LOOP
 *
 * CASCADE — "When I do something to Project, do it to related entities too"
 *   CascadeType.ALL on tasks means:
 *     delete project → delete all tasks   (cascade delete)
 *     save project → save all new tasks   (cascade save)
 *     This makes sense: tasks can't exist without a project.
 *
 * INTERVIEW Q: What is a join table? Why does @ManyToMany need one?
 * A: In a @ManyToOne, the "many" side stores the foreign key (projects.owner_id).
 *    But @ManyToMany is symmetric — which table stores the keys?
 *    Answer: neither. A 3rd "bridge" table stores both sides' keys.
 *    @JoinTable tells JPA what to name it and what columns to use.
 *
 * INTERVIEW Q: What does mappedBy mean?
 * A: It means "the OTHER side owns this relationship. Look there for the @JoinColumn."
 *    In Project: @OneToMany(mappedBy = "project") — "Task.project owns the FK column"
 *    The Task entity has @ManyToOne @JoinColumn(name = "project_id") — that's the owner.
 *    Rule: only ONE side can own the relationship and have the @JoinColumn.
 *    The side with mappedBy is the "inverse" side — it's read-only for the mapping.
 *
 * WHY @Getter @Setter instead of @Data for entities?
 *   @Data includes @EqualsAndHashCode and @ToString.
 *   @EqualsAndHashCode with lazy-loaded collections causes:
 *     1. Hibernate exceptions (accessing unloaded proxy outside transaction)
 *     2. Infinite loops (Project.toString() → tasks.toString() → project.toString())
 *   Solution: exclude relationship fields from toString/equals, OR use @Getter @Setter @Builder manually.
 * ============================================================
 */
@Entity
@Table(name = "projects", indexes = {
        // owner_id is a FK column — index it for "get projects by owner" queries
        @Index(name = "idx_project_owner",  columnList = "owner_id"),
        // status and priority are used in JPA Specification search filters
        @Index(name = "idx_project_status", columnList = "status"),
        @Index(name = "idx_project_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Basic Fields ----

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;
    // @Builder.Default tells Lombok: when building via builder, use this default value.
    // Without it, builder would set status = null even if field has a default.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private LocalDate startDate;       // When work begins
    private LocalDate targetEndDate;   // Planned end date
    private LocalDate actualEndDate;   // When it actually finished (set on COMPLETED)

    // ---- RELATIONSHIP 1: @ManyToOne — Owner ----
    //
    // MANY projects can have ONE owner.
    // The "owner_id" column goes in the PROJECTS table.
    //
    // FetchType.LAZY: Don't load the User object until getOwner() is called.
    //   Without LAZY: every project query would JOIN to users table automatically.
    //
    // @JoinColumn(name = "owner_id"):
    //   Tell JPA: "The FK column is named 'owner_id' in the projects table."
    //   Without this, JPA would generate a column name like "owner_id" anyway,
    //   but being explicit is better practice.
    //
    // nullable = false: A project MUST have an owner. Can't be null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude // Prevent Lombok @ToString from loading this lazy proxy
    private User owner;

    // ---- RELATIONSHIP 2: @ManyToMany — Members ----
    //
    // MANY projects can have MANY members.
    // JPA creates a JOIN TABLE called "project_members".
    //
    // project_members table:
    //   | project_id (FK → projects.id) | user_id (FK → users.id) |
    //
    // joinColumns = the FK column pointing back to THIS entity (Project)
    // inverseJoinColumns = the FK column pointing to the OTHER entity (User)
    //
    // We use Set (not List) for members:
    //   Set: no duplicates. A user can't be a member twice. Makes semantic sense.
    //   List: allows duplicates. Wrong for membership.
    //
    // @Builder.Default: initialize to empty HashSet so builder doesn't create null
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<User> members = new HashSet<>();

    // ---- RELATIONSHIP 3: @OneToMany — Tasks ----
    //
    // ONE project has MANY tasks.
    // The FK column "project_id" lives in the TASKS table, NOT here.
    //
    // mappedBy = "project":
    //   "Look at Task.project field — it owns the JOIN COLUMN (project_id in tasks table)"
    //   This side (Project) is the INVERSE side — it has no DB column.
    //
    // cascade = CascadeType.ALL:
    //   When you save/delete a Project, cascade to all its Tasks.
    //   Delete project → all tasks deleted automatically (orphan cleanup).
    //   orphanRemoval = true: if you remove a task from the list, it's also deleted from DB.
    //
    // orphanRemoval = true:
    //   project.getTasks().remove(task) → deletes the task from DB on next flush
    //   Without this, removing from list would only break the in-memory association.
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Task> tasks = new ArrayList<>();

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

    // ---- Helper Methods ----
    // These small utility methods make service code cleaner.

    public void addMember(User user) {
        members.add(user);
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public boolean isOwnedBy(User user) {
        return this.owner.getId().equals(user.getId());
    }

    public boolean hasMember(User user) {
        return members.stream().anyMatch(m -> m.getId().equals(user.getId()));
    }
}
