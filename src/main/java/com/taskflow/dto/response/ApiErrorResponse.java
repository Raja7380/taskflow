package com.taskflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API ERROR RESPONSE — Consistent error format for ALL API errors.
 *
 * Every error your API returns will follow this structure:
 * {
 *     "status": 400,
 *     "error": "Bad Request",
 *     "message": "Validation failed",
 *     "timestamp": "2024-01-15T10:30:00",
 *     "fieldErrors": {
 *         "email": "Please provide a valid email address",
 *         "password": "Password must be at least 8 characters"
 *     }
 * }
 *
 * WHY a consistent error format?
 *   1. Frontend can have ONE error handler for ALL API errors
 *   2. Easy to debug — always know where to look
 *   3. Professional — real companies always have standardized errors
 *
 * @JsonInclude(NON_NULL) = don't include null fields in JSON response.
 *   If fieldErrors is null (non-validation error), it won't appear in the response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> fieldErrors; // Only present for validation errors
}
