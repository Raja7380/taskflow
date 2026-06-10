package com.taskflow.service;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Priority;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * TASK SERVICE — Business logic for tasks
 * ============================================================
 *
 * AUTHORIZATION RULES FOR TASKS:
 *
 * CREATE task  → any project member or owner can create
 * VIEW task    → any project member or owner can view
 * UPDATE task  → the assignee OR the project owner can update
 * DELETE task  → only the project owner can delete
 *
 * These rules mirror real tools like Jira:
 *   - Developers update their own tasks (change status to IN_PROGRESS, DONE)
 *   - PMs/owners can delete tasks, reassign them, etc.
 *
 * REPORTER is always the person who called create — set from JWT.
 * This can't be spoofed: we ignore any "reporterId" in the request body.
 * ============================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * Create a new task inside a project.
     *
     * IMPORTANT: reporter = currentUser (extracted from JWT, not from request body).
     * The request contains assigneeId (optional) — loaded from DB by ID.
     *
     * AUTHORIZATION: currently any authenticated user can create tasks.
     * In a stricter system, you'd verify the currentUser is a member of the project.
     * We'll add that check in Session 6 (Advanced Queries + Specifications).
     */
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        // Load the project this task belongs to
        var project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        // Build the task entity
        Task.TaskBuilder taskBuilder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .project(project)
                .reporter(currentUser);  // Always the current user — can't be overridden

        // Set assignee only if provided (tasks start unassigned if no assigneeId given)
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            taskBuilder.assignee(assignee);
        }

        Task savedTask = taskRepository.save(taskBuilder.build());
        return TaskResponse.fromEntity(savedTask);
    }

    /**
     * Get a single task by ID.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        Task task = findTaskOrThrow(taskId);
        return TaskResponse.fromEntity(task);
    }

    /**
     * Get all tasks in a project, ordered newest first.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return taskRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all tasks assigned to the current user, ordered by due date.
     * This is the "My Tasks" / personal dashboard view.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {
        return taskRepository.findByAssigneeOrderByDueDateAsc(currentUser)
                .stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update a task.
     *
     * AUTHORIZATION:
     *   - The assigned user can update their own task (change status to IN_PROGRESS/DONE)
     *   - The project owner can update any task in their project
     *   - Anyone else → 403 Forbidden
     *
     * PATCH PATTERN: only update non-null fields from request.
     */
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);

        checkTaskUpdatePermission(task, currentUser);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null) task.setEstimatedHours(request.getEstimatedHours());
        if (request.getActualHours() != null) task.setActualHours(request.getActualHours());

        // Handle re-assignment — only project owner can reassign tasks
        if (request.getAssigneeId() != null) {
            if (!task.getProject().isOwnedBy(currentUser)) {
                throw new AccessDeniedException("Only the project owner can reassign tasks");
            }
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            task.setAssignee(newAssignee);
        }

        // No explicit save() — Hibernate dirty checking handles the UPDATE
        return TaskResponse.fromEntity(task);
    }

    /**
     * Delete a task — only the project OWNER can delete tasks.
     * (The assignee can CANCEL a task by setting status to CANCELLED,
     *  but only the owner can permanently delete it.)
     */
    public void deleteTask(Long taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);

        if (!task.getProject().isOwnedBy(currentUser)) {
            throw new AccessDeniedException("Only the project owner can delete tasks");
        }

        taskRepository.delete(task);
    }

    // ---- Private Helpers ----

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
    }

    private void checkTaskUpdatePermission(Task task, User currentUser) {
        boolean isAssignee = task.getAssignee() != null &&
                task.getAssignee().getId().equals(currentUser.getId());
        boolean isProjectOwner = task.getProject().isOwnedBy(currentUser);
        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());

        if (!isAssignee && !isProjectOwner && !isReporter) {
            throw new AccessDeniedException(
                    "You don't have permission to update this task. " +
                    "Only the assignee, reporter, or project owner can update it."
            );
        }
    }
}
