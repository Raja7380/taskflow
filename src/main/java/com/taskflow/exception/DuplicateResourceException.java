package com.taskflow.exception;

/**
 * Thrown when trying to create a resource that already exists (e.g., duplicate email).
 * Handled by GlobalExceptionHandler -> returns 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
