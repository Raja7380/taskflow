package com.taskflow.event;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import lombok.Getter;

/**
 * EVENT — Published when a task's status changes (state machine transition).
 * Listener notifies the reporter: "Task [title] moved from [old] to [new]."
 */
@Getter
public class TaskStatusChangedEvent {

    private final Task task;
    private final TaskStatus previousStatus;
    private final TaskStatus newStatus;
    private final User changedBy;

    public TaskStatusChangedEvent(Task task, TaskStatus previousStatus,
                                  TaskStatus newStatus, User changedBy) {
        this.task = task;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }
}
