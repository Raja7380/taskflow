package com.taskflow.event;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import lombok.Getter;

/**
 * EVENT — Published when a new task is created.
 *
 * This is a plain Java object (POJO). Spring 4.2+ allows any POJO as an event.
 * No need to extend ApplicationEvent (though you can).
 *
 * HOW EVENTS WORK:
 *   1. Publisher calls: applicationEventPublisher.publishEvent(new TaskCreatedEvent(...))
 *   2. Spring finds all listeners that accept TaskCreatedEvent as parameter
 *   3. Spring calls each listener method with this event object
 *   4. Listener reacts (creates notification, sends email, etc.)
 *
 * The publisher (TaskService) and listener (NotificationListener) are
 * completely decoupled — they don't know about each other.
 */
@Getter
public class TaskCreatedEvent {

    private final Task task;
    private final User creator;

    public TaskCreatedEvent(Task task, User creator) {
        this.task = task;
        this.creator = creator;
    }
}
