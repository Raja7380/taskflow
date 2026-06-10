package com.taskflow.dto.request;

import com.taskflow.entity.Priority;
import com.taskflow.entity.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for updating an existing task — all fields optional (PATCH behavior).
 *
 * TYPICAL USE CASES:
 *   1. Developer picks up a task:
 *      { "status": "IN_PROGRESS" }
 *
 *   2. PM re-assigns a task:
 *      { "assigneeId": 7 }
 *
 *   3. Developer finishes task and logs actual time:
 *      { "status": "DONE", "actualHours": 6 }
 *
 *   4. PM extends deadline:
 *      { "dueDate": "2026-08-01" }
 *
 * Each update call only changes what was sent.
 * Everything not in the request body stays unchanged.
 */
@Data
public class UpdateTaskRequest {

    @Size(min = 2, max = 300, message = "Title must be between 2 and 300 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private TaskStatus status;    // null = don't change

    private Priority priority;    // null = don't change

    private LocalDate dueDate;    // null = don't change

    @Min(value = 1, message = "Estimated hours must be at least 1")
    private Integer estimatedHours;

    @Min(value = 0, message = "Actual hours cannot be negative")
    private Integer actualHours;

    private Long assigneeId;      // null = don't change. Use -1 or a separate unassign endpoint to unassign.
}
