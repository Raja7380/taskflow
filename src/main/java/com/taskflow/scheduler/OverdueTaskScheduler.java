package com.taskflow.scheduler;

import com.taskflow.entity.NotificationType;
import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * OVERDUE TASK SCHEDULER -- Detects tasks past their due date.
 *
 * Runs every hour. When a task's dueDate is before today AND it's not DONE/CANCELLED,
 * it creates an overdue notification for the assignee.
 *
 * WHY HOURLY (not daily)?
 *   A task could become overdue at any hour if its due date just passed midnight.
 *   Hourly checks balance freshness vs. database load.
 *   In production, you'd fine-tune this based on your traffic patterns.
 *
 * fixedDelay = 3600000 (ms) = 1 hour:
 *   Runs 1 hour AFTER the previous run FINISHES (not after it starts).
 *   If the job takes 5 minutes, the next run starts 1 hour + 5 minutes later.
 *   This prevents overlapping runs if the job is slow.
 *
 * fixedDelay vs fixedRate:
 *   fixedDelay = wait N ms AFTER previous run ends (safer for long-running jobs)
 *   fixedRate  = start every N ms regardless of when previous run ended
 *                (can overlap if previous run takes longer than N ms)
 *
 * initialDelay = 60000 (1 minute):
 *   Wait 1 minute after app startup before first run.
 *   Prevents the scheduler from hammering the DB while the app is still initializing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OverdueTaskScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    @Transactional(readOnly = true)
    public void detectAndNotifyOverdueTasks() {
        log.info("[SCHEDULER] Running overdue task detection job");
        LocalDate today = LocalDate.now();

        List<Task> overdueTasks = taskRepository.findOverdueTasks(today);
        log.info("[SCHEDULER] Found {} overdue tasks", overdueTasks.size());

        for (Task task : overdueTasks) {
            String message = String.format(
                    "OVERDUE: Task \"%s\" in project '%s' was due on %s and is not yet complete.",
                    task.getTitle(),
                    task.getProject().getName(),
                    task.getDueDate()
            );

            notificationService.create(
                    task.getAssignee().getId(),
                    message,
                    NotificationType.TASK_OVERDUE,
                    task.getId(),
                    "Task"
            );
        }

        log.info("[SCHEDULER] Overdue detection complete -- {} notifications sent", overdueTasks.size());
    }
}
