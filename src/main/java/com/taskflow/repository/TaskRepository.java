package com.taskflow.repository;

import com.taskflow.entity.Project;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TASK REPOSITORY — Data access layer for Task entity.
 *
 * ALL METHODS USE SPRING DATA DERIVED QUERIES:
 *
 * Spring Data reads the method name and generates SQL automatically.
 * Grammar: findBy[Field][Condition]And[Field][Condition]
 *
 * METHOD                           │ GENERATED SQL
 * ─────────────────────────────────┼───────────────────────────────────────────
 * findByProject(project)           │ WHERE project_id = ?
 * findByAssignee(user)             │ WHERE assignee_id = ?
 * findByReporter(user)             │ WHERE reporter_id = ?
 * findByProjectAndStatus(p, s)     │ WHERE project_id = ? AND status = ?
 * findByAssigneeAndStatus(u, s)    │ WHERE assignee_id = ? AND status = ?
 * countByProject(project)          │ SELECT COUNT(*) WHERE project_id = ?
 *
 * INTERVIEW Q: How does Spring Data JPA know what SQL to generate?
 * A: It parses the method name using a keyword grammar:
 *    - findBy, deleteBy, countBy → SELECT/DELETE/COUNT
 *    - And, Or → AND/OR conditions
 *    - Containing, StartingWith, GreaterThan → LIKE, >
 *    If parsing fails, it throws an exception at startup (not runtime) — fail-fast.
 *
 * ORDERING: add "OrderBy[Field][Asc/Desc]" to sort results.
 *   findByProjectOrderByCreatedAtDesc → ORDER BY created_at DESC
 */
@Repository
// JpaSpecificationExecutor adds: findAll(Specification, Pageable) — needed for dynamic search
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * @EntityGraph — fixes N+1 for single task lookup.
     *
     * WITHOUT @EntityGraph — TaskResponse.fromEntity() accesses 3 lazy fields:
     *   task.getProject()   → 1 extra query
     *   task.getAssignee()  → 1 extra query
     *   task.getReporter()  → 1 extra query
     *   Total: 4 queries for one task
     *
     * WITH @EntityGraph({"project","assignee","reporter"}):
     *   All 3 @ManyToOne loaded via a single LEFT JOIN query
     *   Total: 1 query
     *
     * @ManyToOne fields are safe to join-fetch together (no cartesian product risk).
     * The N+1 risk is with @OneToMany / @ManyToMany collections (like tasks list in Project).
     */
    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    Optional<Task> findById(Long id);

    // All tasks in a project — load assignee+reporter eagerly (project already known)
    @EntityGraph(attributePaths = {"assignee", "reporter"})
    List<Task> findByProjectOrderByCreatedAtDesc(Project project);

    // Tasks assigned to user — load project+reporter eagerly (assignee already known)
    @EntityGraph(attributePaths = {"project", "reporter"})
    List<Task> findByAssigneeOrderByDueDateAsc(User assignee);

    // All tasks reported/created by a specific user
    List<Task> findByReporter(User reporter);

    // Tasks in a project filtered by status (e.g., all IN_PROGRESS tasks)
    List<Task> findByProjectAndStatus(Project project, TaskStatus status);

    // Tasks assigned to a user filtered by status (e.g., user's TODO tasks)
    List<Task> findByAssigneeAndStatus(User assignee, TaskStatus status);

    // How many tasks are in a project (used in ProjectResponse.taskCount)
    long countByProject(Project project);

    // Used by TaskReminderScheduler: tasks due on a specific date, not finished, with an assignee
    @Query("SELECT t FROM Task t WHERE t.dueDate = :date " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') " +
           "AND t.assignee IS NOT NULL")
    List<Task> findTasksDueOnDate(@Param("date") LocalDate date);

    // Used by OverdueTaskScheduler: tasks past due, not finished, with an assignee
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') " +
           "AND t.assignee IS NOT NULL")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);
}
