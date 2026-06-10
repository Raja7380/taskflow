package com.taskflow.controller;

import com.taskflow.dto.request.CreateProjectRequest;
import com.taskflow.dto.request.UpdateProjectRequest;
import com.taskflow.dto.response.PagedResponse;
import com.taskflow.dto.response.ProjectResponse;
import com.taskflow.entity.Priority;
import com.taskflow.entity.ProjectStatus;
import com.taskflow.entity.User;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * PROJECT CONTROLLER — HTTP endpoints for projects
 * ============================================================
 *
 * BASE URL: /api/projects
 *
 * ENDPOINTS OVERVIEW:
 *   POST   /api/projects             → create new project
 *   GET    /api/projects             → get my projects (owned or member)
 *   GET    /api/projects/all         → get all projects (admin only)
 *   GET    /api/projects/{id}        → get one project by ID
 *   PUT    /api/projects/{id}        → update a project
 *   DELETE /api/projects/{id}        → delete a project
 *   POST   /api/projects/{id}/members/{userId}   → add member
 *   DELETE /api/projects/{id}/members/{userId}   → remove member
 *
 * THE STAR OF THIS SESSION: @AuthenticationPrincipal
 *
 *   @AuthenticationPrincipal User currentUser
 *
 *   This annotation tells Spring Security:
 *   "Inject the currently authenticated user into this method parameter."
 *
 *   HOW IT WORKS:
 *   1. Request arrives with "Authorization: Bearer eyJhbGc..."
 *   2. JwtAuthenticationFilter runs, validates token
 *   3. Filter calls: SecurityContextHolder.getContext().setAuthentication(auth)
 *   4. The "auth" contains our User object as the "principal"
 *   5. @AuthenticationPrincipal extracts that User object from the context
 *
 *   WHY NOT GET USER FROM SERVICE?
 *   You COULD call userService.getCurrentUser() inside the service.
 *   But that couples the service to the SecurityContext (harder to test).
 *   Passing the User as a parameter makes services testable with any User object.
 *
 *   WHY User not UserDetails?
 *   Our User entity implements UserDetails. So @AuthenticationPrincipal
 *   injects the actual User entity, not a generic UserDetails.
 *   Spring casts it automatically since User IS-A UserDetails.
 *
 * INTERVIEW Q: What is @AuthenticationPrincipal?
 * A: A shortcut to access the authenticated principal stored in Spring Security's
 *    SecurityContextHolder. It avoids boilerplate:
 *    (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
 *    becomes simply: @AuthenticationPrincipal User user
 * ============================================================
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
@SecurityRequirement(name = "bearerAuth")  // Tells Swagger: all endpoints need JWT
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project.
     * The authenticated user automatically becomes the owner.
     *
     * POST /api/projects
     * Body: { "name": "My App", "priority": "HIGH", ... }
     * Returns: 201 Created + the created project
     */
    @PostMapping
    @Operation(summary = "Create a new project", description = "The authenticated user becomes the project owner")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {

        ProjectResponse response = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects where the current user is owner OR member.
     *
     * GET /api/projects
     * Returns: 200 OK + list of projects
     */
    @GetMapping
    @Operation(summary = "Get my projects", description = "Returns all projects where you are owner or member")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(projectService.getMyProjects(currentUser));
    }

    /**
     * Get ALL projects — admin only.
     * @PreAuthorize is NOT on this method — the SecurityConfig already handles
     * /api/admin/** → hasRole('ADMIN'). But we use a different path here.
     * We'll add @PreAuthorize in Session 5 (Method Security).
     *
     * GET /api/projects/all
     */
    @GetMapping("/all")
    @Operation(summary = "Get all projects (admin only)")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    /**
     * Search projects with optional filters + pagination.
     *
     * GET /api/projects/search
     * GET /api/projects/search?status=ACTIVE&keyword=taskflow&page=0&size=10
     */
    @GetMapping("/search")
    @Operation(summary = "Search projects with filters and pagination")
    public ResponseEntity<PagedResponse<ProjectResponse>> searchProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(
                projectService.searchProjects(status, priority, keyword, page, size, sortBy, sortDir));
    }

    /**
     * Get a single project by ID.
     *
     * GET /api/projects/1
     * Returns: 200 OK + project, or 404 if not found
     */
    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    /**
     * Update a project — only the owner can update.
     *
     * PUT /api/projects/1
     * Body: { "name": "New Name", "status": "ACTIVE" }
     * Returns: 200 OK + updated project, or 403 if not owner, or 404 if not found
     *
     * NOTE: We use PUT path for update, but the service does PATCH behavior (partial updates).
     * Purists would use PATCH HTTP method for partial updates.
     * In practice, most teams use PUT for both full and partial updates — keep it simple.
     */
    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project", description = "Only the project owner can update")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(projectService.updateProject(projectId, request, currentUser));
    }

    /**
     * Delete a project — only the owner can delete.
     * Cascade deletes all tasks in the project.
     *
     * DELETE /api/projects/1
     * Returns: 204 No Content (success, nothing to return)
     */
    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project", description = "Only the project owner can delete. Also deletes all tasks.")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {

        projectService.deleteProject(projectId, currentUser);
        return ResponseEntity.noContent().build();
        // 204 No Content — success, but nothing to return in body
    }

    /**
     * Add a member to a project.
     *
     * POST /api/projects/1/members/5
     * Returns: 200 OK + updated project (with new memberCount)
     */
    @PostMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Add a member to a project", description = "Only the project owner can add members")
    public ResponseEntity<ProjectResponse> addMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(projectService.addMember(projectId, userId, currentUser));
    }

    /**
     * Remove a member from a project.
     *
     * DELETE /api/projects/1/members/5
     * Returns: 200 OK + updated project (with reduced memberCount)
     */
    @DeleteMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Remove a member from a project", description = "Only the project owner can remove members")
    public ResponseEntity<ProjectResponse> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(projectService.removeMember(projectId, userId, currentUser));
    }
}
