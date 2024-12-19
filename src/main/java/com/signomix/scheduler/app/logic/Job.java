package com.signomix.scheduler.app.logic;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.inject.Inject;

public class Job {

    @Inject
    @Channel("commands")
    Emitter<String> commandsEmitter;

    @Inject
    @Channel("devicecommands")
    Emitter<String> deviceCommandsEmitter;

    @Inject
    @Channel("events")
    Emitter<String> eventsEmitter;

    @Channel("adminemail")
    Emitter<String> adminEmailEmitter;
    

    
    protected void checkAndReschedule(long taskDefinitionId) {
        //TODO: implement task rescheduling
        // 1. get task definition from database
        // 2. if task definition is not found or not enabled remove it from scheduler
        // 3. if task definition is found and enabled, check modification date and reschedule if updated
    }

    protected void sendEmail(String email, String subject, String message) {
        //TODO: implement email sending
    }

    protected void publish(String topic, String message) {
        switch (topic) {
            case "commands":
                commandsEmitter.send(message);
                break;
            case "devicecommands":
                deviceCommandsEmitter.send(message);
                break;
            case "events":
                eventsEmitter.send(message);
                break;
            case "adminemail":
                adminEmailEmitter.send(message);
                break;
            default:
                break;
        }
    }
}
