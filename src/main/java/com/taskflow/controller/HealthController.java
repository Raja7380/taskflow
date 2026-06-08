package com.taskflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * HEALTH CONTROLLER — Simple endpoint to check if the app is running.
 *
 * This is a PUBLIC endpoint (configured in SecurityConfig).
 * Useful for:
 *   1. Docker health checks: HEALTHCHECK CMD curl -f http://localhost:8080/api/health
 *   2. Load balancer health checks
 *   3. Quick "is it running?" verification
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Application health check")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Check if the application is running")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "TaskFlow",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
