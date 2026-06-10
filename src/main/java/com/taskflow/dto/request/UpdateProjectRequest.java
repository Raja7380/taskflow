package com.taskflow.dto.request;

import com.taskflow.entity.Priority;
import com.taskflow.entity.ProjectStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for updating an existing project.
 *
 * ALL FIELDS ARE OPTIONAL — This is a "PATCH-style" update.
 * Only fields the client sends will be updated.
 * Fields not included in the request stay unchanged.
 *
 * PATCH vs PUT:
 *   PUT  = replace entire resource. All fields required.
 *          If you omit "description", it becomes null/empty.
 *   PATCH = partial update. Only change what you send.
 *           If you omit "description", it stays as-is.
 *
 * We implement PATCH behavior in the service:
 *   if (request.getName() != null) project.setName(request.getName());
 *
 * WHAT CANNOT BE UPDATED HERE:
 *   - owner    → can't change ownership via update (separate flow or admin only)
 *   - members  → separate dedicated endpoints (POST /add-member, DELETE /remove-member)
 *   - id       → never changeable
 *   - createdAt → immutable audit field
 */
@Data
public class UpdateProjectRequest {

    @Size(min = 2, max = 200, message = "Project name must be between 2 and 200 characters")
    private String name;             // null = don't change

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;      // null = don't change

    private ProjectStatus status;    // null = don't change

    private Priority priority;       // null = don't change

    private LocalDate targetEndDate;     // null = don't change

    private LocalDate actualEndDate;     // Set this when status → COMPLETED
}
