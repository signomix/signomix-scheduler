package com.signomix.scheduler.adapters.driven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.dto.TaskDefinition;

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
    public void createDatabase() {
        tasks= new HashMap<>();
        // Quarkus cron format: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html

        // Report task - dummy
        TaskDefinition task = new TaskDefinition();
        task.id=1L;
        task.type = TaskDefinition.REPORT;
        task.jobName = "job1";
        task.jobGroup = "reports";
        task.scheduleDefinition = "0 0/1 * * * ?"; // Every minute
        task.nlScheduleDefinition = "Every minute";
        task.triggerName = "trigger1";
        task.triggerGroup = "reports";
        task.jobDataMap.put("dql", "report DummyReport");
        addTask(task);

        // Event task - dummy
        task = new TaskDefinition();
        task.id=2L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "job2";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/2 * * * ?"; // Every 2 minutes
        task.nlScheduleDefinition = "Every 2 minutes";
        task.triggerName = "trigger2";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "system-monitor");
        addTask(task);
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
    
}

