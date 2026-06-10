package com.taskflow.specification;

import com.taskflow.entity.Priority;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * PROJECT SPECIFICATION — Dynamic filtering for projects.
 * Same pattern as TaskSpecification — each method is one reusable WHERE condition.
 */
public class ProjectSpecification {

    private ProjectSpecification() {}

    /**
     * Filter by project status.
     * SQL: WHERE status = 'ACTIVE'
     */
    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    /**
     * Filter by priority.
     * SQL: WHERE priority = 'HIGH'
     */
    public static Specification<Project> hasPriority(Priority priority) {
        return (root, query, cb) ->
                priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    /**
     * Filter by keyword in project name (case-insensitive).
     * SQL: WHERE LOWER(name) LIKE '%taskflow%'
     */
    public static Specification<Project> nameContains(String keyword) {
        return (root, query, cb) ->
                (keyword == null || keyword.isBlank()) ? null :
                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    /**
     * Filter by owner ID.
     * SQL: WHERE owner_id = 1
     */
    public static Specification<Project> ownedBy(Long ownerId) {
        return (root, query, cb) ->
                ownerId == null ? null : cb.equal(root.get("owner").get("id"), ownerId);
    }
}
