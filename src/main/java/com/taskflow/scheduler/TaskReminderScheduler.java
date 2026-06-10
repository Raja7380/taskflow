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
 * TASK REMINDER SCHEDULER -- Background jobs for due-date notifications.
 *
 * WHY SCHEDULING EXISTS:
 *   Some work doesn't happen because a user clicked a button.
 *   It happens on a TIMER -- regardless of user activity.
 *
 *   Real examples:
 *   - Gmail: "You have 3 unread emails" -- reminder after 3 days of inactivity
 *   - Jira: "Task X is due tomorrow" -- automated reminder at 9 AM
 *   - Swiggy: "Your order hasn't moved in 20 mins" -- triggered by a timer
 *   - Banks: monthly statement generated on the 1st of every month
 *   - GitHub Actions CI: run tests every night at 2 AM (nightly builds)
 *   - Amazon: "Items in your cart for 7 days" -- re-engagement reminder
 *
 * @Scheduled CRON SYNTAX -- "0 0 9 * * *"
 *   Position 1 (seconds):      0
 *   Position 2 (minutes):      0
 *   Position 3 (hours):        9
 *   Position 4 (day of month): * (every day)
 *   Position 5 (month):        * (every month)
 *   Position 6 (day of week):  * (every day of week)
 *
 *   More examples:
 *     "0 30 8 * * MON-FRI"  -- 8:30 AM weekdays only
 *     "0 0 0 1 * *"         -- midnight on 1st of every month
 *     "0 0 * * * *"         -- every hour on the hour
 *
 * IMPORTANT: @EnableScheduling on TaskFlowApplication.java activates scheduling.
 * Without it, @Scheduled annotations are ignored.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    /**
     * Every day at 9 AM: find tasks due TODAY and notify assignees.
     *
     * Why 9 AM? Developers check notifications at the start of their workday.
     * Sending at 9 AM gives them time to act during work hours.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDueTodayReminders() {
        log.info("[SCHEDULER] Running due-today reminder job");
        LocalDate today = LocalDate.now();

        List<Task> tasksDueToday = taskRepository.findTasksDueOnDate(today);
        log.info("[SCHEDULER] Found {} tasks due today", tasksDueToday.size());

        for (Task task : tasksDueToday) {
            String message = String.format("REMINDER: Task \"%s\" in project '%s' is due TODAY!",
                    task.getTitle(), task.getProject().getName());

            notificationService.create(
                    task.getAssignee().getId(),
                    message,
                    NotificationType.TASK_DUE_SOON,
                    task.getId(),
                    "Task"
            );
        }

        log.info("[SCHEDULER] Due-today reminder complete -- {} notifications sent", tasksDueToday.size());
    }

    /**
     * Every day at 9:05 AM: remind about tasks due TOMORROW.
     * Gives developers a 24-hour heads-up.
     */
    @Scheduled(cron = "0 5 9 * * *")
    @Transactional(readOnly = true)
    public void sendDueTomorrowReminders() {
        log.info("[SCHEDULER] Running due-tomorrow reminder job");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Task> tasksDueTomorrow = taskRepository.findTasksDueOnDate(tomorrow);

        for (Task task : tasksDueTomorrow) {
            String message = String.format("Task \"%s\" in project '%s' is due TOMORROW.",
                    task.getTitle(), task.getProject().getName());

            notificationService.create(
                    task.getAssignee().getId(),
                    message,
                    NotificationType.TASK_DUE_SOON,
                    task.getId(),
                    "Task"
            );
        }

        log.info("[SCHEDULER] Due-tomorrow reminder complete -- {} notifications sent", tasksDueTomorrow.size());
    }
}
