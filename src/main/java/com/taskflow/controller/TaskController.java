package com.taskflow.controller;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.PagedResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Priority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.List;

/**
 * ============================================================
 * TASK CONTROLLER — HTTP endpoints for tasks
 * ============================================================
 *
 * BASE URL: /api/tasks
 *
 * ENDPOINTS OVERVIEW:
 *   POST   /api/tasks                         → create a task
 *   GET    /api/tasks/my                      → my assigned tasks
 *   GET    /api/tasks/{id}                    → get one task
 *   GET    /api/tasks/project/{projectId}     → all tasks in a project
 *   PUT    /api/tasks/{id}                    → update a task
 *   DELETE /api/tasks/{id}                    → delete a task
 *
 * NOTICE: projectId is inside the CreateTaskRequest body, not the URL.
 * Alternative design: POST /api/projects/{projectId}/tasks
 * Both are valid REST patterns. We use a flat /api/tasks for simplicity.
 * Session 6 revisits API design conventions.
 * ============================================================
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task.
     * The authenticated user becomes the reporter.
     * assigneeId in the body is optional.
     *
     * POST /api/tasks
     * Body: { "title": "Fix bug #123", "projectId": 1, "priority": "HIGH" }
     * Returns: 201 Created
     */
    @PostMapping
    @Operation(summary = "Create a new task", description = "Current user becomes the reporter. assigneeId is optional.")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        TaskResponse response = taskService.createTask(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all tasks assigned to the current user.
     * The "My Tasks" / personal dashboard endpoint.
     *
     * GET /api/tasks/my
     * Returns: 200 OK + list ordered by due date
     */
    @GetMapping("/my")
    @Operation(summary = "Get my assigned tasks", description = "Returns tasks where you are the assignee, ordered by due date")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getMyTasks(currentUser));
    }

    /**
     * Get a single task by ID.
     *
     * GET /api/tasks/42
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    /**
     * Get all tasks in a specific project.
     * Ordered newest first (latest tasks show at top).
     *
     * GET /api/tasks/project/1
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all tasks in a project", description = "Returns tasks ordered by creation date, newest first")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    /**
     * Update a task.
     * Assignee, reporter, or project owner can update.
     *
     * PUT /api/tasks/42
     * Body: { "status": "IN_PROGRESS" }
     * Returns: 200 OK + updated task
     */
    @PutMapping("/{taskId}")
    @Operation(summary = "Update a task",
               description = "Assignee, reporter, or project owner can update. " +
                             "Only the project owner can reassign (change assigneeId).")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(taskService.updateTask(taskId, request, currentUser));
    }

    /**
     * Search tasks with optional filters + pagination.
     *
     * ALL parameters are optional. Combine any of them freely.
     *
     * GET /api/tasks/search
     * GET /api/tasks/search?status=IN_PROGRESS
     * GET /api/tasks/search?priority=HIGH&keyword=bug
     * GET /api/tasks/search?projectId=1&status=TODO&page=0&size=10
     * GET /api/tasks/search?assigneeId=5&sortBy=dueDate&sortDir=asc
     *
     * @RequestParam(required = false) — parameter is optional. If not sent, Spring injects null.
     * @RequestParam(defaultValue = "0") — if not sent, defaults to 0.
     * @DateTimeFormat — tells Spring how to parse a date string from the URL.
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search tasks with filters and pagination",
        description = "All parameters optional. Combine any filters. Example: ?status=IN_PROGRESS&priority=HIGH&page=0&size=20"
    )
    public ResponseEntity<PagedResponse<TaskResponse>> searchTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<TaskResponse> response = taskService.searchTasks(
                status, priority, keyword, projectId, assigneeId,
                dueBefore, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a task — only the project owner can delete.
     * (Assignees should CANCEL tasks, not delete them — audit trail matters.)
     *
     * DELETE /api/tasks/42
     * Returns: 204 No Content
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task", description = "Only the project owner can permanently delete tasks")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {

        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
