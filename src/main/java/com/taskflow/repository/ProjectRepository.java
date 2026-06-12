package com.taskflow.repository;

import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PROJECT REPOSITORY — Data access layer for Project entity.
 *
 * INHERITS FROM JpaRepository<Project, Long>:
 *   - save(project)         → INSERT or UPDATE
 *   - findById(id)          → SELECT by ID, returns Optional<Project>
 *   - findAll()             → SELECT * (all projects)
 *   - deleteById(id)        → DELETE by ID
 *   - existsById(id)        → SELECT EXISTS
 *   - count()               → SELECT COUNT(*)
 *   All other CRUD methods built-in — no SQL needed.
 *
 * CUSTOM QUERIES:
 *
 * METHOD 1 — findByOwner(user):
 *   Spring Data JPA "Derived Query" — reads the method name and generates SQL.
 *   "findBy" + "Owner" → WHERE owner = ?
 *   Generated SQL: SELECT * FROM projects WHERE owner_id = ?
 *   No @Query annotation needed — the name alone does it!
 *
 * METHOD 2 — findByOwnerOrMember(user):
 *   We want: "projects where user is owner OR user is a member"
 *   Spring Data can't auto-derive this (involves a JOIN TABLE for members).
 *   Solution: JPQL (@Query) — Object-Oriented SQL.
 *
 *   JPQL vs SQL:
 *     SQL:   SELECT * FROM projects p JOIN project_members pm ON p.id = pm.project_id WHERE p.owner_id = ? OR pm.user_id = ?
 *     JPQL:  SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members
 *     JPQL uses Java class/field names, not table/column names.
 *     "MEMBER OF p.members" = element exists in the collection (handles the JOIN TABLE).
 *
 *   DISTINCT: without it, if a user is both owner AND member, the project appears twice.
 *
 * INTERVIEW Q: What's the difference between SQL and JPQL?
 * A: SQL works with tables and columns. JPQL works with entity classes and fields.
 *    Hibernate translates JPQL → SQL for the specific database dialect.
 *    This makes JPQL database-agnostic — same query works on PostgreSQL, MySQL, Oracle.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    /**
     * @EntityGraph — fixes the N+1 problem for single project lookups.
     *
     * WITHOUT @EntityGraph:
     *   getProjectById(1) generates:
     *     SELECT * FROM projects WHERE id = 1           (1 query)
     *     SELECT * FROM users WHERE id = ?              (1 lazy query for owner)
     *     SELECT * FROM project_members WHERE project_id = 1  (1 lazy query for members)
     *   = 3 queries for a single project
     *
     * WITH @EntityGraph({"owner", "members"}):
     *   getProjectById(1) generates:
     *     SELECT p.*, u.*, pm.* FROM projects p
     *       LEFT JOIN users u ON p.owner_id = u.id
     *       LEFT JOIN project_members pm ON p.id = pm.project_id
     *       LEFT JOIN users u2 ON pm.user_id = u2.id
     *     WHERE p.id = 1
     *   = 1 query with JOINs (owner and members loaded together)
     *
     * NOTE: We override the JpaRepository.findById() method here.
     * Spring Data JPA allows re-declaring inherited methods with additional annotations.
     * tasks is kept LAZY here -- a separate count query is acceptable for a single project.
     */
    @EntityGraph(attributePaths = {"owner", "members"})
    Optional<Project> findById(Long id);

    // Find all projects owned by this user
    // @EntityGraph loads owner eagerly to avoid N separate owner queries for a list
    @EntityGraph(attributePaths = {"owner"})
    List<Project> findByOwner(User owner);

    // Find all projects where user is owner OR member
    // @EntityGraph loads owner only -- members/tasks are lazy (acceptable for lists)
    @EntityGraph(attributePaths = {"owner"})
    @Query("SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    List<Project> findByOwnerOrMember(@Param("user") User user);

    // Existence check — used to validate project access before operations
    boolean existsByIdAndOwner(Long id, User owner);
}
