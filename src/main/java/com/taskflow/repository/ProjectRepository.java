package com.taskflow.repository;

import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    // Find all projects owned by this user
    // Generated SQL: SELECT * FROM projects WHERE owner_id = :owner_id
    List<Project> findByOwner(User owner);

    // Find all projects where user is owner OR member
    // @Param("user") maps the :user placeholder to the method parameter
    @Query("SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    List<Project> findByOwnerOrMember(@Param("user") User user);

    // Existence check — used to validate project access before operations
    boolean existsByIdAndOwner(Long id, User owner);
}
