package com.signomix.scheduler.app.ports.driving;

import java.util.List;

import com.signomix.common.User;
import com.signomix.scheduler.dto.TaskDefinition;

public interface ForScheduler {

    public void reloadSystemTasks(User user);

    public void reloadTask(long taskId, User user);

    public TaskDefinition getTask(long taskId, User user);

    public List<TaskDefinition> getTasks(User user, Integer offset, Integer limit);

    public TaskDefinition createTask(TaskDefinition task, User user);

    public TaskDefinition updateTask(TaskDefinition task, User user);

}
