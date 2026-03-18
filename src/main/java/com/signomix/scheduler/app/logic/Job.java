package com.signomix.scheduler.app.logic;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.quartz.JobDataMap;

import jakarta.inject.Inject;

public class Job {

    @Inject
    @Channel("commands")
    Emitter<String> commandsEmitter;

    @Inject
    @Channel("devicecommands")
    Emitter<String> deviceCommandsEmitter;

    @Channel("adminemail")
    Emitter<String> adminEmailEmitter;

    @Channel("email")
    Emitter<String> emailEmitter;

    @Inject
    TaskRunner taskRunner;

    protected void reschedule(JobDataMap dataMap) {
        taskRunner.reschedule(dataMap);
    }

    protected void sendEmail(String email, String subject, String content, String attachmentFileName) {
        String message = email + "\n" +
                subject + "\n" +
                attachmentFileName + "\n" +
                content;

        publish("email", message);
    }

    protected void publish(String topic, String message) {
        switch (topic) {
            case "commands":
                commandsEmitter.send(message);
                break;
            case "devicecommands":
                deviceCommandsEmitter.send(message);
                break;
            case "adminemail":
                adminEmailEmitter.send(message);
                break;
            case "email":
                emailEmitter.send(message);
                break;
            default:
                break;
        }
    }
}
