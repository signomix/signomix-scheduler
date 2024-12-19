package com.signomix.scheduler.app.ports.driven;

import java.util.List;

import com.signomix.scheduler.dto.TaskDefinition;

public interface ForAccessTasksDatabase {


    public void createDatabase();
    public List<TaskDefinition> getTasks();
    public long addTask(TaskDefinition task);
    public TaskDefinition getTask(long taskId);
    public void updateTask(TaskDefinition task);
    public void deleteTask(long taskId);
    
}
