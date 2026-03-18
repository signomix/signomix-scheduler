package com.signomix.scheduler.app.logic;

import org.jboss.logging.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;

public class EventJob extends Job implements  org.quartz.Job {

    @Inject
    Logger logger;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long id = (Long) context.getMergedJobDataMap().get("id");
        reschedule(context.getMergedJobDataMap());
        String channel = (String) context.getMergedJobDataMap().get("channel");
        if(channel == null || channel.isEmpty()){
            logger.error("No channel defined for event job "+id);
            return;
        }
        String message = buildMesage(context.getMergedJobDataMap());
        if(message == null || message.isEmpty()){
            logger.error("No event message defined for event job");
            return;
        }
        logger.info("Sending event : " + channel + " with message: " + message);
        publish(channel, message);
    }

    private String buildMesage(JobDataMap dataMap) {
        StringBuilder sb = new StringBuilder();
        String messageType = dataMap.getString("type");
        // for backward compatibility
        if(dataMap.getString("message") != null && !dataMap.getString("message").isEmpty()) {
            messageType = dataMap.getString("message").toLowerCase();
        }
        if (messageType == null || messageType.isEmpty()) {
            logger.error("No message type defined for event job");
            return null;
        }
        sb.append("type=").append(messageType).append(";");
        dataMap.forEach((key, value) -> {
            if (!"message".equals(key) && !"id".equals(key) && !"channel".equals(key) && !"type".equals(key)) {
                sb.append(key).append("=").append(value).append(";");
            }
        });
        return sb.toString();
    }
}
