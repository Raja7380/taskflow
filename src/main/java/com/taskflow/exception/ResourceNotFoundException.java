package com.taskflow.exception;

/**
 * Thrown when a requested resource (User, Task, Project) is not found.
 * Handled by GlobalExceptionHandler -> returns 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }

    // e.g., new ResourceNotFoundException("Project", "id", 42L)
    // Message: "Project not found with id: 42"
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName + " not found with " + fieldName + ": " + fieldValue);
    }
}
