package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;

public class ReportJob extends Job implements  org.quartz.Job {

    @Inject
    Logger logger;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long id = (Long) context.getMergedJobDataMap().get("id");
        checkAndReschedule(id);
        String dql = (String) context.getMergedJobDataMap().get("dql");
        if(dql == null || dql.isEmpty()){
            logger.error("No DQL query defined for report job "+id);
            return;
        }
        String email = (String) context.getMergedJobDataMap().get("email");
        if(email == null || email.isEmpty()){
            logger.error("No target email defined for report job");
            return;
        }
        logger.info("Executing report job "+id+" with DQL query: " + dql);
        logger.info("Not implemented yet");
        //TODO: call the report service
        
        //TODO: send the report by email
        String subject = "Report";
        String message = "The report is not implemented yet";
        sendEmail(email, subject, message);

    }

    
}
