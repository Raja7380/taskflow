package com.taskflow.service;

import com.taskflow.dto.response.AuditLogResponse;
import com.taskflow.entity.AuditLog;
import com.taskflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUDIT LOG SERVICE
 *
 * The key design here: save() uses Propagation.REQUIRES_NEW.
 *
 * WHY THIS MATTERS:
 * Scenario: A user calls createProject(). This runs in a @Transactional context.
 * Inside the AOP aspect, we call auditLogService.save() to record the action.
 *
 * Case 1 — createProject() SUCCEEDS:
 *   Without REQUIRES_NEW: audit log saves inside the SAME transaction → committed together. OK.
 *   With REQUIRES_NEW: audit log saves in a NEW transaction → committed independently. Also OK.
 *
 * Case 2 — createProject() THROWS an exception:
 *   Without REQUIRES_NEW: the outer transaction ROLLS BACK → audit log is ALSO rolled back.
 *     Result: the failure is NOT recorded. We have no evidence the attempt happened. BAD.
 *   With REQUIRES_NEW: audit log transaction already COMMITTED before the outer rollback.
 *     Result: the failure IS recorded. We can see "user X tried to do Y and it failed." GOOD.
 *
 * Audit logs MUST survive failures. That's their entire purpose.
 * Propagation.REQUIRES_NEW = "suspend the current transaction, start a brand new one,
 * commit it, then resume the outer transaction."
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByUser(Long userId, int page, int size) {
        return auditLogRepository
                .findByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, size))
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAll(int page, int size) {
        return auditLogRepository
                .findAll(PageRequest.of(page, size, Sort.by("timestamp").descending()))
                .map(AuditLogResponse::fromEntity);
    }
}
