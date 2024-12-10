package com.signomix.scheduler.adapters.driven;

import java.util.ArrayList;
import java.util.List;

import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.dto.TaskDefinition;

public class DummyTaskDatabase implements ForAccessTasksDatabase {

    // There will be 2 groupsof tasks:
    // - system tasks (see: signomix-ta-jobs)
    // - user tasks (user defined)

    private ArrayList<TaskDefinition> tasks;

    public DummyTaskDatabase() {
        // Quarkus cron format: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
        
        tasks= new ArrayList<>();

        // Report task - dummy
        TaskDefinition task = new TaskDefinition();
        task.type = TaskDefinition.REPORT;
        task.jobName = "job1";
        task.jobGroup = "reports";
        task.scheduleDefinition = "0 0/1 * * * ?"; // Every minute
        task.nlScheduleDefinition = "Every minute";
        task.triggerName = "trigger1";
        task.triggerGroup = "reports";
        task.jobDataMap.put("dql", "report DummyReport");
        tasks.add(task);

        // Event task - dummy
        task = new TaskDefinition();
        task.type = TaskDefinition.EVENT;
        task.jobName = "job2";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/2 * * * ?"; // Every 2 minutes
        task.nlScheduleDefinition = "Every 2 minutes";
        task.triggerName = "trigger2";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "system-monitor");
        tasks.add(task);

    }

    @Override
    public List<TaskDefinition> getTasks() {
        return tasks;
    }
    
}

