package com.signomix.scheduler.dto;

import java.beans.Transient;
import java.util.Map;

public class TaskDefinition {
    public final static int REPORT = 0; // Run a report
    public final static int EVENT = 1; // Trigger an event
    public final static int DATA_WEBHOOK = 2; // Read data from external source
    public final static int WEBHOOK = 3; // Call an external service (webhook)
    public final static int EMAIL = 4; // Send an email
    public final static int SMS = 5; // Send an SMS
    public final static int SYS_COMMAND = 6; // Execute a system command

    public Long id;
    public int type;
    public String userId;
    public boolean enabled = true;
    public String nlScheduleDefinition; // Schedule definition in natural language
    public String scheduleDefinition;   // Schedule definition in cron format
    public Map<String, String> jobDataMap = new java.util.HashMap<>();
    public String description;

    @Transient
    public String getJobName() {
        return "job"+id;
    }

    @Transient
    public String getTriggerName() {
        return "trigger"+id;
    }

    @Transient
    public String getJobGroup() {
        switch (type) {
            case EVENT:
                return "event";
            case REPORT:
                return "report";
            case DATA_WEBHOOK:
                return "data_webhook";
            case WEBHOOK:
                return "webhook";
            case EMAIL:
                return "email";
            case SMS:
                return "sms";
            case SYS_COMMAND:
                return "sys_command";
            default:
                return "default";   
        }
    }

    @Transient
    public String getTriggerGroup() {
        switch (type) {
            case EVENT:
                return "event";
            case REPORT:
                return "report";
            case DATA_WEBHOOK:
                return "data_webhook";
            case WEBHOOK:
                return "webhook";
            case EMAIL:
                return "email";
            case SMS:
                return "sms";
            case SYS_COMMAND:
                return "sys_command";
            default:
                return "default";   
        }
    }



}
