package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;

public class EventJob extends Job implements  org.quartz.Job {

    @Inject
    Logger logger;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long id = (Long) context.getMergedJobDataMap().get("id");
        checkAndReschedule(id);
        String channel = (String) context.getMergedJobDataMap().get("channel");
        if(channel == null || channel.isEmpty()){
            logger.error("No channel defined for event job "+id);
            return;
        }
        String message = (String) context.getMergedJobDataMap().get("message");
        if(message == null || message.isEmpty()){
            logger.error("No event message defined for event job");
            return;
        }
        logger.info("Sending event : " + channel + " with message: " + message);
        logger.info("Not implemented yet"); 
        publish(channel, message);
    }
}
