package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;

public class DummyJob implements  org.quartz.Job {

    @Inject
    Logger logger;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing job with JobDataMap:");
        context.getMergedJobDataMap().forEach((k, v) -> {
            logger.info("Key: " + k + " Value: " + v);
        });
    }

    
}
