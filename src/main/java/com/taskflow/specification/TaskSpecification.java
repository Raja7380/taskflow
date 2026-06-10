package com.taskflow.specification;

import com.taskflow.entity.Priority;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * ============================================================
 * TASK SPECIFICATION — Dynamic query building with JPA Criteria API
 * ============================================================
 *
 * WHAT IS A SPECIFICATION?
 *   A Specification is a reusable piece of a WHERE clause.
 *   Each static method here is one condition. You combine them to build queries.
 *
 *   Think of it like LEGO blocks:
 *     hasStatus(IN_PROGRESS)       → WHERE status = 'IN_PROGRESS'
 *     hasPriority(HIGH)            → WHERE priority = 'HIGH'
 *     titleContains("bug")         → WHERE LOWER(title) LIKE '%bug%'
 *
 *   Combine them:
 *     hasStatus AND hasPriority AND titleContains
 *     → WHERE status = 'IN_PROGRESS' AND priority = 'HIGH' AND LOWER(title) LIKE '%bug%'
 *
 * WHY SPECIFICATIONS EXIST:
 *   Problem: A search API can have many optional filters.
 *   Without Specifications, you'd need a separate repository method for every combination:
 *     findByStatus(status)
 *     findByStatusAndPriority(status, priority)
 *     findByStatusAndPriorityAndProjectId(status, priority, projectId)
 *     findByStatusAndProjectIdAndKeyword(status, projectId, keyword)
 *     ... 2^N combinations (N = number of filters)
 *   With 6 filters → 64 repository methods. Impossible to maintain.
 *
 *   With Specifications: one method in the repository handles ALL combinations.
 *   You just compose the right conditions at runtime based on which params were sent.
 *
 * HOW THE LAMBDA WORKS:
 *   Specification<Task> is a functional interface with one method:
 *     Predicate toPredicate(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb)
 *
 *   Root<Task> root   = represents the Task table/entity (root.get("status") = the status column)
 *   CriteriaBuilder cb = factory for building conditions (cb.equal, cb.like, cb.lessThan)
 *   Predicate         = one WHERE condition
 *
 *   The lambda is the implementation:
 *     (root, query, cb) -> cb.equal(root.get("status"), status)
 *     is the same as:
 *     WHERE tasks.status = ?
 *
 * RETURNING NULL from a Specification = "no condition" (ignored when combined).
 * This lets you safely pass null for any filter — it's just skipped.
 * ============================================================
 */
public class TaskSpecification {

    // Private constructor — this is a utility class (only static methods, no instances)
    private TaskSpecification() {}

    /**
     * Filter by status.
     * SQL: WHERE status = 'IN_PROGRESS'
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    /**
     * Filter by priority.
     * SQL: WHERE priority = 'HIGH'
     */
    public static Specification<Task> hasPriority(Priority priority) {
        return (root, query, cb) ->
                priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    /**
     * Filter by project ID.
     * root.get("project") navigates the @ManyToOne relationship.
     * root.get("project").get("id") = the project's id field.
     * SQL: WHERE project_id = 1
     */
    public static Specification<Task> belongsToProject(Long projectId) {
        return (root, query, cb) ->
                projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    /**
     * Filter by assignee ID.
     * SQL: WHERE assignee_id = 5
     */
    public static Specification<Task> isAssignedTo(Long userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("assignee").get("id"), userId);
    }

    /**
     * Filter by keyword in title (case-insensitive).
     * cb.lower() = SQL LOWER() function
     * cb.like() = SQL LIKE
     * "%" is the SQL wildcard — matches any characters before/after the keyword.
     * SQL: WHERE LOWER(title) LIKE '%bug%'
     */
    public static Specification<Task> titleContains(String keyword) {
        return (root, query, cb) ->
                (keyword == null || keyword.isBlank()) ? null :
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }

    /**
     * Filter tasks due on or before a date.
     * SQL: WHERE due_date <= '2026-07-01'
     */
    public static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) ->
                date == null ? null : cb.lessThanOrEqualTo(root.get("dueDate"), date);
    }

    /**
     * Filter tasks with NO assignee (unassigned tasks).
     * cb.isNull() = SQL IS NULL
     * SQL: WHERE assignee_id IS NULL
     */
    public static Specification<Task> isUnassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }
}
