package com.taskflow.event;

import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import lombok.Getter;

/**
 * EVENT — Published when a new project is created.
 * Can be used to: send welcome notifications, initialize default data, trigger analytics.
 */
@Getter
public class ProjectCreatedEvent {

    private final Project project;
    private final User creator;

    public ProjectCreatedEvent(Project project, User creator) {
        this.project = project;
        this.creator = creator;
    }
}
