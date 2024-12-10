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

    ForAccessTasksDatabase jobDatabase=new DummyTaskDatabase();

    void onStart(@Observes StartupEvent event) throws SchedulerException {
        logger.info("The application is starting...");
        jobDatabase.getTasks().forEach(task -> {
            switch (task.type) {
                case TaskDefinition.REPORT:
                    runReportTask(task);
                    break;
                case TaskDefinition.EVENT:
                    runEventTask(task);
                    break;
                default:
                    logger.error("Unsupported job type: " + task.type);
            }
        });
    }

    public void runReportTask(TaskDefinition definition) {
        logger.info("Scheduling task: " + definition.jobName + " with schedule: " + definition.scheduleDefinition);
        JobDataMap jobDataMap = new JobDataMap();
        definition.jobDataMap.forEach((k, v) -> {
            jobDataMap.put(k, v);
        });
        JobDetail job = JobBuilder.newJob(ReportJob.class)
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

    public void runEventTask(TaskDefinition definition) {
        logger.info("Scheduling task: " + definition.jobName + " with schedule: " + definition.scheduleDefinition);
        JobDataMap jobDataMap = new JobDataMap();
        definition.jobDataMap.forEach((k, v) -> {
            jobDataMap.put(k, v);
        });
        JobDetail job = JobBuilder.newJob(ReportJob.class)
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

}
