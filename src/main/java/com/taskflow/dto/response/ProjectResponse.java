package com.taskflow.dto.response;

import com.taskflow.entity.Priority;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning project data to the client.
 *
 * WHY NOT RETURN THE PROJECT ENTITY DIRECTLY?
 *   Entity has:
 *     - Lazy collections (tasks, members) → Jackson triggers Hibernate proxy → LazyInitializationException
 *     - Circular references (Project → Task → Project → ...) → Stack overflow
 *     - Too much data (owner's password hash exposed accidentally)
 *     - Future DB schema changes break the API contract
 *
 * WHAT THIS DTO FLATTENS:
 *   Instead of returning the entire owner object, we return:
 *     ownerName: "Raja Singh"     ← extracted from User.fullName
 *     ownerEmail: "r@gmail.com"   ← extracted from User.email
 *   This avoids returning the owner's password hash etc.
 *
 *   Instead of returning all tasks (could be hundreds), we return:
 *     taskCount: 14               ← just the number
 *     memberCount: 3              ← just the number
 *
 * This is called "PROJECTION" — showing a subset/transformation of the real data.
 *
 * The static fromEntity() method is the MAPPER.
 * It converts Project entity → ProjectResponse DTO.
 * No external library needed for simple cases.
 * In Session 6, we'll use MapStruct to auto-generate mappers.
 */
@Data
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private Priority priority;
    private LocalDate startDate;
    private LocalDate targetEndDate;
    private LocalDate actualEndDate;

    // Flattened owner info — NOT the full User object
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;

    private int memberCount;  // How many members in this project
    private int taskCount;    // How many tasks in this project

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * MAPPER METHOD — Convert a Project entity to this DTO.
     *
     * Called in service layer:
     *   return ProjectResponse.fromEntity(project);
     *
     * WHY STATIC?
     *   No need to create a ProjectResponse object first.
     *   ProjectResponse.fromEntity(project) reads naturally.
     *
     * NOTE on members.size() and tasks.size():
     *   These access lazy collections — MUST be called within an active transaction.
     *   That's why services are @Transactional.
     *   Outside a transaction, Hibernate closes the session and lazy loading fails.
     */
    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .priority(project.getPriority())
                .startDate(project.getStartDate())
                .targetEndDate(project.getTargetEndDate())
                .actualEndDate(project.getActualEndDate())
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getFullName())
                .ownerEmail(project.getOwner().getEmail())
                .memberCount(project.getMembers().size())
                .taskCount(project.getTasks().size())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
