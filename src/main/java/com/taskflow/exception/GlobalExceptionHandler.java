package com.taskflow.exception;

import com.taskflow.dto.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER — Catches ALL exceptions across ALL controllers.
 *
 * WITHOUT this, Spring returns ugly default error pages.
 * WITH this, every error follows our clean ApiErrorResponse format.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   @ControllerAdvice = "Apply this to ALL controllers"
 *   @ResponseBody = "Return JSON, not HTML"
 *
 * HOW IT WORKS:
 *   1. Any controller throws an exception
 *   2. Spring intercepts it BEFORE it reaches the client
 *   3. Finds the matching @ExceptionHandler method here
 *   4. Calls that method, returns its response to the client
 *
 * ORDER MATTERS: More specific exceptions first, generic Exception last.
 *
 * INTERVIEW Q: What's the difference between @ControllerAdvice and @RestControllerAdvice?
 * A: @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 *    @ControllerAdvice can return views (HTML). @RestControllerAdvice always returns JSON.
 *    For REST APIs, always use @RestControllerAdvice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid validation failures.
     * When @Valid @RequestBody fails, Spring throws MethodArgumentNotValidException.
     * We extract each field error and return them in a clean format.
     *
     * Example response:
     * {
     *     "status": 400,
     *     "error": "Validation Failed",
     *     "message": "One or more fields are invalid",
     *     "fieldErrors": {
     *         "email": "Please provide a valid email address",
     *         "password": "Password must be at least 8 characters"
     *     }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * CATCH-ALL — Any unhandled exception ends up here.
     * Returns 500 Internal Server Error with a generic message.
     * NEVER expose stack traces or internal details to the client!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        // Log the full error internally (for debugging)
        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
