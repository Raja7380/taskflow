package com.taskflow.service;

import com.taskflow.annotation.Auditable;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.response.PagedResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Priority;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.event.TaskAssignedEvent;
import com.taskflow.event.TaskCreatedEvent;
import com.taskflow.event.TaskStatusChangedEvent;
import com.taskflow.exception.InvalidStateTransitionException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.specification.TaskSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ============================================================
 * TASK SERVICE — Session 3 additions:
 *   1. State machine: valid status transitions
 *   2. Dynamic search with Specifications + Pagination
 * ============================================================
 *
 * STATE MACHINE — What valid transitions look like:
 *
 *   TODO ──────────────→ IN_PROGRESS
 *   TODO ──────────────→ CANCELLED
 *   IN_PROGRESS ───────→ IN_REVIEW
 *   IN_PROGRESS ───────→ TODO  (unstarted — rare but valid)
 *   IN_PROGRESS ───────→ CANCELLED
 *   IN_REVIEW ─────────→ DONE
 *   IN_REVIEW ─────────→ IN_PROGRESS  (reviewer sends back for more work)
 *   IN_REVIEW ─────────→ CANCELLED
 *   DONE ──────────────→ (nothing — terminal state)
 *   CANCELLED ─────────→ (nothing — terminal state)
 *
 * Invalid: DONE → TODO, CANCELLED → IN_PROGRESS, etc.
 * These throw InvalidStateTransitionException → 400 Bad Request.
 * ============================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================
    // STATE MACHINE — Map of: current status → allowed next statuses
    // ============================================================
    // Map.of() creates an unmodifiable map — can't be accidentally changed at runtime.
    // Set.of() creates an unmodifiable set.
    // Using unmodifiable collections for constants is a best practice.
    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS = Map.of(
            TaskStatus.TODO,        Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_REVIEW, TaskStatus.TODO, TaskStatus.CANCELLED),
            TaskStatus.IN_REVIEW,   Set.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            TaskStatus.DONE,        Set.of(),       // terminal — no transitions allowed
            TaskStatus.CANCELLED,   Set.of()        // terminal — no transitions allowed
    );

    /**
     * Create a new task inside a project.
     */
    @Auditable(action = "CREATE_TASK", entityType = "Task")
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        var project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        Task.TaskBuilder taskBuilder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .project(project)
                .reporter(currentUser);

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            taskBuilder.assignee(assignee);
        }

        Task savedTask = taskRepository.save(taskBuilder.build());
        // Publish event — NotificationListener will create a notification for the project owner
        eventPublisher.publishEvent(new TaskCreatedEvent(savedTask, currentUser));
        return TaskResponse.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        return TaskResponse.fromEntity(findTaskOrThrow(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return taskRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream().map(TaskResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {
        return taskRepository.findByAssigneeOrderByDueDateAsc(currentUser)
                .stream().map(TaskResponse::fromEntity).collect(Collectors.toList());
    }

    @Auditable(action = "DELETE_TASK", entityType = "Task")
    public void deleteTask(Long taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        if (!task.getProject().isOwnedBy(currentUser)) {
            throw new AccessDeniedException("Only the project owner can delete tasks");
        }
        taskRepository.delete(task);
    }

    /**
     * UPDATE TASK — Now includes state machine validation.
     *
     * When the request changes the status field, we check if the transition is valid
     * BEFORE applying any other changes. If invalid, throw immediately.
     */
    @Auditable(action = "UPDATE_TASK", entityType = "Task")
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        checkTaskUpdatePermission(task, currentUser);

        // STATE MACHINE: validate and publish status change event
        if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
            TaskStatus previousStatus = task.getStatus();
            validateStatusTransition(previousStatus, request.getStatus());
            task.setStatus(request.getStatus());
            eventPublisher.publishEvent(new TaskStatusChangedEvent(task, previousStatus, request.getStatus(), currentUser));
        }

        if (request.getTitle() != null)           task.setTitle(request.getTitle());
        if (request.getDescription() != null)     task.setDescription(request.getDescription());
        if (request.getPriority() != null)        task.setPriority(request.getPriority());
        if (request.getDueDate() != null)         task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null)  task.setEstimatedHours(request.getEstimatedHours());
        if (request.getActualHours() != null)     task.setActualHours(request.getActualHours());

        if (request.getAssigneeId() != null) {
            if (!task.getProject().isOwnedBy(currentUser)) {
                throw new AccessDeniedException("Only the project owner can reassign tasks");
            }
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            task.setAssignee(newAssignee);
            eventPublisher.publishEvent(new TaskAssignedEvent(task, newAssignee, currentUser));
        }

        return TaskResponse.fromEntity(task);
    }

    // ============================================================
    // SEARCH — Dynamic filtering + Pagination (Session 3 new method)
    // ============================================================

    /**
     * Search tasks with any combination of filters + pagination.
     *
     * Any parameter can be null — null means "no filter on this field."
     * Only the filters that have values are added to the WHERE clause.
     *
     * Examples:
     *   searchTasks(IN_PROGRESS, null, null, 1L, null, ...)
     *     → WHERE status = 'IN_PROGRESS' AND project_id = 1
     *
     *   searchTasks(null, HIGH, "bug", null, null, ...)
     *     → WHERE priority = 'HIGH' AND LOWER(title) LIKE '%bug%'
     *
     *   searchTasks(null, null, null, null, null, ...)
     *     → (no WHERE clause — returns all tasks, paginated)
     *
     * HOW SPECIFICATION.WHERE(NULL) WORKS:
     *   Specification.where(null) starts with "no conditions".
     *   .and(someSpec) adds a condition only if someSpec is not null.
     *   If a Specification method returns null (because the filter param is null),
     *   .and(null) is ignored — the condition is simply not added.
     *
     * @param page     0-indexed page number (first page = 0)
     * @param size     number of records per page
     * @param sortBy   field name to sort by (e.g., "createdAt", "dueDate", "priority")
     * @param sortDir  "asc" or "desc"
     */
    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> searchTasks(
            TaskStatus status,
            Priority priority,
            String keyword,
            Long projectId,
            Long assigneeId,
            LocalDate dueBefore,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        // Build the sort direction
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // Build the Pageable object — tells Spring Data which page and sort to use
        Pageable pageable = PageRequest.of(page, size, sort);

        // Compose the Specification from individual conditions
        // Specification.where(null) = start with no conditions
        // .and(...) = add condition (if the spec returns null, it's a no-op)
        Specification<Task> spec = Specification
                .where(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.hasPriority(priority))
                .and(TaskSpecification.titleContains(keyword))
                .and(TaskSpecification.belongsToProject(projectId))
                .and(TaskSpecification.isAssignedTo(assigneeId))
                .and(TaskSpecification.dueBefore(dueBefore));

        // Execute the query — returns a Page<Task> (not all tasks, just the requested page)
        Page<Task> taskPage = taskRepository.findAll(spec, pageable);

        // Map each Task entity to TaskResponse DTO, then wrap in PagedResponse
        Page<TaskResponse> responsePage = taskPage.map(TaskResponse::fromEntity);
        return PagedResponse.from(responsePage);
    }

    // ---- Private Helpers ----

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
    }

    /**
     * Validates that a status transition is allowed by the state machine.
     * Throws InvalidStateTransitionException if not allowed.
     */
    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        Set<TaskStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(currentStatus, newStatus);
        }
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
