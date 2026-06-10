package com.taskflow.repository;

import com.taskflow.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    // Find all failed actions (for security monitoring)
    List<AuditLog> findBySuccessFalseAndTimestampAfter(LocalDateTime since);

    // Find all actions by a user in a time range (compliance queries)
    Page<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
