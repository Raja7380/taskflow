package com.taskflow.controller;

import com.taskflow.dto.response.AuditLogResponse;
import com.taskflow.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit trail — who did what and when")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/audit-logs — all audit logs (admin only).
     * @PreAuthorize is method-level security — checked before the method runs.
     * Requires @EnableMethodSecurity (already on SecurityConfig).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit logs (admin only)")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogService.getAll(page, size));
    }

    /**
     * GET /api/audit-logs/my — current user's own audit trail.
     * Users can see what actions were logged for their account.
     */
    @GetMapping("/my")
    @Operation(summary = "Get my audit logs")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.taskflow.entity.User currentUser) {
        return ResponseEntity.ok(auditLogService.getByUser(currentUser.getId(), page, size));
    }
}
