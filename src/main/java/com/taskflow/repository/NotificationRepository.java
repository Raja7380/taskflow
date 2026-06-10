package com.taskflow.repository;

import com.taskflow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Only unread notifications (for the notification badge count)
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    // Count unread — used for the red badge number in the UI
    long countByUserIdAndReadFalse(Long userId);

    // Mark ALL notifications as read for a user (bulk update — no need to load each one)
    // @Modifying = tells Spring this is an UPDATE/DELETE query, not a SELECT
    // @Query with JPQL UPDATE — more efficient than loading all and saving each
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
