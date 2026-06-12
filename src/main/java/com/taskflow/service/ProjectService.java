package com.taskflow.service;

import com.taskflow.annotation.Auditable;
import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.response.PagedResponse;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.entity.Priority;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectStatus;
import com.taskflow.entity.User;
import com.taskflow.event.ProjectCreatedEvent;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.specification.ProjectSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * PROJECT SERVICE — Business logic for projects
 * ============================================================
 *
 * @Transactional — Why is this crucial here?
 *
 * The @Transactional annotation starts a database transaction for the method.
 * A transaction is a "unit of work" — all DB operations inside it either:
 *   SUCCESS → all changes committed to DB
 *   FAILURE → all changes rolled back (like they never happened)
 *
 * WHY DO WE NEED IT FOR LAZY LOADING?
 *   Hibernate uses a "Session" to load lazy data.
 *   Session is open during a transaction.
 *   @Transactional on service method = Session is open for the whole method.
 *   When service method returns, Session closes.
 *
 *   Without @Transactional:
 *     findById() returns a Project (Session closes here)
 *     project.getMembers() → Session is CLOSED → LazyInitializationException!
 *
 *   With @Transactional:
 *     findById() returns a Project
 *     project.getMembers() → Session still OPEN → works fine
 *     method returns → Session closes → transaction committed
 *
 * readOnly = true:
 *   For methods that only READ data (no INSERT/UPDATE/DELETE):
 *   - Hibernate skips the "dirty checking" step (comparing entity state to DB state)
 *   - Slightly faster — PostgreSQL can route to a read replica
 *   - Signals developer intent: "this method must not modify data"
 *
 * @AuthenticationPrincipal Pattern:
 *   The currently logged-in user is passed into service methods as a parameter.
 *   The controller extracts it from Spring Security context using @AuthenticationPrincipal.
 *   Services get the User object — no security context access needed in service layer.
 *   This keeps services TESTABLE — you can pass any User object in unit tests.
 *
 * INTERVIEW Q: What is @Transactional and when do you need it?
 * A: @Transactional ensures all DB operations in a method execute atomically.
 *    Need it when: (1) multiple DB operations should succeed/fail together,
 *    (2) accessing lazy-loaded relationships, (3) any modification to entities.
 *    The default propagation is REQUIRED — joins existing transaction or creates new one.
 * ============================================================
 */
@Service
@RequiredArgsConstructor
@Transactional  // All methods are transactional by default
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new project. The logged-in user becomes the owner.
     *
     * FLOW:
     * 1. Build a Project entity from the request DTO
     * 2. Set the owner = currentUser (from JWT, NOT from request body)
     * 3. Save to DB → Hibernate generates INSERT statement
     * 4. Return ProjectResponse DTO (not the entity)
     */
    @Auditable(action = "CREATE_PROJECT", entityType = "Project")
    public ProjectResponse createProject(CreateProjectRequest request, User currentUser) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .startDate(request.getStartDate())
                .targetEndDate(request.getTargetEndDate())
                .owner(currentUser)
                // status defaults to PLANNING via @Builder.Default in entity
                .build();

        Project savedProject = projectRepository.save(project);
        eventPublisher.publishEvent(new ProjectCreatedEvent(savedProject, currentUser));
        return ProjectResponse.fromEntity(savedProject);
    }

    /**
     * Get a single project by ID.
     *
     * @Cacheable(value = "projects", key = "#projectId"):
     *   FIRST request:  Cache miss → DB query → store result in Redis → return
     *   NEXT requests:  Cache hit  → return from Redis (no DB query!)
     *
     * Key format in Redis: "taskflow:projects::1" (prefix + cache + "::" + key)
     * TTL: 10 minutes (configured in RedisConfig)
     *
     * WHY CACHE DTOs NOT ENTITIES?
     *   If we cached Project entities, the entity would be detached from Hibernate
     *   when retrieved from cache. Any lazy loading would throw LazyInitializationException.
     *   ProjectResponse (DTO) has no Hibernate dependencies — safe to cache forever.
     *   Rule: Only cache serializable DTOs, never JPA entities.
     */
    @Cacheable(value = "projects", key = "#projectId")
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Get all projects where the current user is owner OR member.
     * This is the "My Projects" endpoint.
     *
     * Uses a custom JPQL query in ProjectRepository because Spring Data
     * can't auto-derive "owner OR MEMBER OF members" from a method name.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(User currentUser) {
        return projectRepository.findByOwnerOrMember(currentUser)
                .stream()
                .map(ProjectResponse::fromEntity)
                // ↑ Method reference — same as: project -> ProjectResponse.fromEntity(project)
                .collect(Collectors.toList());
    }

    /**
     * Get ALL projects in the system — admin-only feature.
     * The @PreAuthorize("hasRole('ADMIN')") check is on the controller,
     * but we could also add it here.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update a project — only the OWNER can do this.
     *
     * PATCH pattern: only update fields that are non-null in the request.
     * This avoids accidentally clearing fields the user didn't intend to change.
     *
     * AUTHORIZATION CHECK:
     *   If currentUser is not the owner → throw AccessDeniedException → 403 Forbidden
     *   This is business-level security (not Spring Security filter-level).
     */
    // @CacheEvict removes the cached entry for this project after update.
    // Next call to getProjectById(projectId) will re-query DB and cache fresh data.
    @CacheEvict(value = "projects", key = "#projectId")
    @Auditable(action = "UPDATE_PROJECT", entityType = "Project")
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, User currentUser) {
        Project project = findProjectOrThrow(projectId);

        checkOwnership(project, currentUser, "update");

        // Only update fields that were actually sent (null = don't change)
        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        if (request.getPriority() != null) project.setPriority(request.getPriority());
        if (request.getTargetEndDate() != null) project.setTargetEndDate(request.getTargetEndDate());
        if (request.getActualEndDate() != null) project.setActualEndDate(request.getActualEndDate());

        // No explicit save() needed here!
        // Because project is a MANAGED entity (loaded within this @Transactional method),
        // Hibernate's "dirty checking" detects changes and generates UPDATE SQL automatically.
        // When the transaction commits (method returns), Hibernate flushes changes to DB.
        // This is called "automatic dirty checking" — one of Hibernate's core features.

        return ProjectResponse.fromEntity(project);
    }

    /**
     * Delete a project — only the OWNER can do this.
     * CascadeType.ALL on tasks means all tasks are deleted automatically.
     */
    @CacheEvict(value = "projects", key = "#projectId")
    @Auditable(action = "DELETE_PROJECT", entityType = "Project")
    public void deleteProject(Long projectId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        checkOwnership(project, currentUser, "delete");
        projectRepository.delete(project);
    }

    /**
     * Add a member to a project — only the OWNER can add members.
     *
     * FLOW:
     * 1. Load project (throw 404 if missing)
     * 2. Check current user is owner (throw 403 if not)
     * 3. Load the user to add (throw 404 if missing)
     * 4. Check user isn't already a member (idempotent — adding twice is a no-op)
     * 5. Add to members Set → @JoinTable handles the INSERT into project_members
     */
    // Adding a member changes memberCount in the cached ProjectResponse — evict it
    @CacheEvict(value = "projects", key = "#projectId")
    public ProjectResponse addMember(Long projectId, Long userId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        checkOwnership(project, currentUser, "add members to");

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Using a Set means adding the same user twice is a no-op (Set ignores duplicates)
        project.addMember(userToAdd);

        // Hibernate dirty checking: project.members changed → INSERT into project_members
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Remove a member from a project — only the OWNER can remove members.
     */
    @CacheEvict(value = "projects", key = "#projectId")
    public ProjectResponse removeMember(Long projectId, Long userId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        checkOwnership(project, currentUser, "remove members from");

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        project.removeMember(userToRemove);

        // Hibernate dirty checking: project.members changed → DELETE from project_members
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Search projects with optional filters + pagination.
     * Same pattern as task search — any param can be null.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> searchProjects(
            ProjectStatus status,
            Priority priority,
            String keyword,
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Project> spec = Specification
                .where(ProjectSpecification.hasStatus(status))
                .and(ProjectSpecification.hasPriority(priority))
                .and(ProjectSpecification.nameContains(keyword));

        Page<Project> projectPage = projectRepository.findAll(spec, pageable);
        Page<ProjectResponse> responsePage = projectPage.map(ProjectResponse::fromEntity);
        return PagedResponse.from(responsePage);
    }

    // ---- Private Helper Methods ----

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private void checkOwnership(Project project, User currentUser, String action) {
        if (!project.isOwnedBy(currentUser)) {
            throw new AccessDeniedException(
                    "You don't have permission to " + action + " this project. Only the project owner can do this."
            );
        }
    }
}
