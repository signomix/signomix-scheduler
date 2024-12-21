package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.signomix.scheduler.adapters.driven.DummyTaskDatabase;
import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.dto.TaskDefinition;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class TaskRunner {

    @Inject
    Logger logger;

    @Inject
    org.quartz.Scheduler quartz;

    ForAccessTasksDatabase jobDatabase = new DummyTaskDatabase();

    void onStart(@Observes StartupEvent event) throws SchedulerException {
        logger.info("The application is starting...");
        jobDatabase.createDatabase();
        createSystemTasks();
        jobDatabase.getTasks().forEach(task -> {
            logger.info("Task: " + task.jobName + " with schedule: " + task.scheduleDefinition);
            scheduleTask(task);
        });
    }


    public void scheduleTask(TaskDefinition definition) {
        if (!definition.enabled) {
            logger.info("Task: " + definition.jobName + " is disabled.");
            return;
        }
        int definitionType = definition.type;
        logger.info("Scheduling task: " + definition.jobName + " with schedule: " + definition.scheduleDefinition);
        JobDataMap jobDataMap = new JobDataMap();
        definition.jobDataMap.forEach((k, v) -> {
            jobDataMap.put(k, v);
        });
        jobDataMap.put("id", definition.id);
        JobBuilder jobBuilder;
        switch (definitionType) {
            case TaskDefinition.EVENT:
                jobBuilder = JobBuilder.newJob(EventJob.class);
                break;
            case TaskDefinition.REPORT:
                jobBuilder = JobBuilder.newJob(ReportJob.class);
                break;
            default:
                return;
        }
        JobDetail job = jobBuilder
                .withIdentity(definition.jobName, definition.jobGroup)
                .setJobData(jobDataMap)
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(definition.triggerName, definition.triggerGroup)
                .startNow()
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(definition.scheduleDefinition))
                .build();
        try {
            quartz.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void createSystemTasks() {
        // Event tasks
        TaskDefinition task;
        
        task = new TaskDefinition();
        task.id = 1L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "system-monitor";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/2 * * * ?"; // Every 2 minutes
        task.nlScheduleDefinition = "Every 2 minutes";
        task.triggerName = "monitor-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "system-monitor");
        jobDatabase.addTask(task);

        task = new TaskDefinition();
        task.id = 2L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "backup";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/5 * * * ?"; // Every 5 minutes
        task.nlScheduleDefinition = "Every 5 minutes";
        task.triggerName = "backup-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "backup");
        jobDatabase.addTask(task);

        task = new TaskDefinition();
        task.id = 3L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "archive";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/10 * * * ?"; // Every 10 minutes
        task.nlScheduleDefinition = "Every 10 minutes";
        task.triggerName = "archive-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "archive");
        jobDatabase.addTask(task);

        task= new TaskDefinition();
        task.id = 4L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "datacleaner";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/15 * * * ?"; // Every 15 minutes
        task.nlScheduleDefinition = "Every 15 minutes";
        task.triggerName = "datacleaner-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "datacleaner");
        jobDatabase.addTask(task);

        task = new TaskDefinition();
        task.id = 5L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "devicechecker";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/20 * * * ?"; // Every 20 minutes
        task.nlScheduleDefinition = "Every 20 minutes";
        task.triggerName = "devicechecker-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "devicechecker");
        jobDatabase.addTask(task);

        task = new TaskDefinition();
        task.id = 6L;
        task.type = TaskDefinition.EVENT;
        task.jobName = "commandrunner";
        task.jobGroup = "events";
        task.scheduleDefinition = "0 0/30 * * * ?"; // Every 30 minutes
        task.nlScheduleDefinition = "Every 30 minutes";
        task.triggerName = "commandrunner-trigger";
        task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "devicecommands");
        jobDatabase.addTask(task);
    }

}
