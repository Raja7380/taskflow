package com.taskflow.event;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import lombok.Getter;

/**
 * EVENT — Published when a task is assigned (or re-assigned) to a user.
 * Listener will notify the new assignee: "You have been assigned to [task]."
 */
@Getter
public class TaskAssignedEvent {

    private final Task task;
    private final User newAssignee;
    private final User assignedBy;  // who made the assignment (project owner)

    public TaskAssignedEvent(Task task, User newAssignee, User assignedBy) {
        this.task = task;
        this.newAssignee = newAssignee;
        this.assignedBy = assignedBy;
    }
}
