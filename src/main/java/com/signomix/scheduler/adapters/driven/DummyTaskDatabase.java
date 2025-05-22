package com.signomix.scheduler.adapters.driven;

import java.util.HashMap;
import java.util.List;

import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.dto.TaskDefinition;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.Dependent;

@Dependent
public class DummyTaskDatabase implements ForAccessTasksDatabase {

    // There will be 2 groupsof tasks:
    // - system tasks (see: signomix-ta-jobs)
    // - user tasks (user defined)

    private HashMap<Long,TaskDefinition> tasks;

    public DummyTaskDatabase() {
                
    }

    @Override
    public List<TaskDefinition> getTasks() {
        return tasks.values().stream().toList();
    }

    @Override
    public List<TaskDefinition> getUserTasks(String userId, Integer organization) {
        return tasks.values().stream().filter(
            t -> (userId==null||userId.isEmpty())?t.userId==null:t.userId.equals(userId)
        ).toList();
    }

    @Override
    public void createDatabase() {
        tasks= new HashMap<>();
        // Quarkus cron format: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
    }

    @Override
    public void backupDb() {
        // No backup needed for dummy database
    }

    @Override
    public long addTask(TaskDefinition task) {
        if(task.id==null){
            task.id=System.currentTimeMillis();
        }
        tasks.put(task.id, task);
        return task.id;
    }

    @Override
    public TaskDefinition getTask(long taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void updateTask(TaskDefinition task) {
        tasks.put(task.id, task);
    }

    @Override
    public void deleteTask(long taskId) {
        tasks.remove(taskId);
    }

    @Override
    public void setDataSource(AgroalDataSource dataSource) {
    }

    @Override
    public int getTaskCount() {
        return tasks.size();
    }
    
}

