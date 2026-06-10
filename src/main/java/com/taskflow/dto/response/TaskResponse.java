package com.taskflow.dto.response;

import com.taskflow.entity.Priority;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning task data to the client.
 *
 * FLATTENING STRATEGY:
 *   Task has 3 @ManyToOne relationships:
 *     project  → we return projectId + projectName (not the full project object)
 *     assignee → we return assigneeId + assigneeName (nullable — could be unassigned)
 *     reporter → we return reporterId + reporterName
 *
 * WHY RETURN IDs?
 *   The frontend may need IDs to make further API calls.
 *   E.g., to navigate to the project page: GET /api/projects/{projectId}
 *   If we only return names, the frontend can't navigate anywhere.
 *
 * NULLABLE FIELDS:
 *   assigneeId and assigneeName can be null if task is unassigned.
 *   Client should handle null gracefully (show "Unassigned" in UI).
 */
@Data
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private Integer estimatedHours;
    private Integer actualHours;

    // Flattened project info
    private Long projectId;
    private String projectName;

    // Flattened assignee info — NULLABLE (task may be unassigned)
    private Long assigneeId;
    private String assigneeName;

    // Flattened reporter info — always present
    private Long reporterId;
    private String reporterName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * MAPPER — Convert Task entity → TaskResponse DTO.
     *
     * Handles the nullable assignee gracefully:
     *   if task.getAssignee() != null → populate assignee fields
     *   else                          → leave null (client handles "Unassigned" display)
     */
    public static TaskResponse fromEntity(Task task) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .reporterId(task.getReporter().getId())
                .reporterName(task.getReporter().getFullName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt());

        // Handle nullable assignee
        if (task.getAssignee() != null) {
            builder.assigneeId(task.getAssignee().getId())
                   .assigneeName(task.getAssignee().getFullName());
        }

        return builder.build();
    }
}
