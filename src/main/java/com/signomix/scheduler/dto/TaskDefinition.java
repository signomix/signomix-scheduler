package com.signomix.scheduler.dto;

import java.util.Map;

public class TaskDefinition {
    public final static int REPORT = 0; // Run a report
    public final static int EVENT = 1; // Trigger an event
    public final static int DATA_WEBHOOK = 2; // Read data from external source
    public final static int WEBHOOK = 3; // Call an external service (webhook)
    public final static int EMAIL = 4; // Send an email
    public final static int SMS = 5; // Send an SMS

    public Long id;
    public int type;
    public String userId;
    public boolean enabled = true;
    public String triggerName;
    public String triggerGroup;
    public String nlScheduleDefinition; // Schedule definition in natural language
    public String scheduleDefinition;   // Schedule definition in cron format
    public String jobName;
    public String jobGroup;
    public Map<String, String> jobDataMap = new java.util.HashMap<>();

}
