package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

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

    void onStart(@Observes StartupEvent event) throws SchedulerException {
        logger.info("The application is starting...");
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("dql", "report Dummy");
        JobDetail job = JobBuilder.newJob(ReportJob.class)
                         .withIdentity("report1", "reports")
                         .setJobData(jobDataMap)
                         .build();
       Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity("trigger1", "reports")
                            .startNow()
                            .withSchedule(
                               SimpleScheduleBuilder.simpleSchedule()
                                  .withIntervalInSeconds(10)
                                  .repeatForever())
                            .build();
       quartz.scheduleJob(job, trigger);
    }
    
}
