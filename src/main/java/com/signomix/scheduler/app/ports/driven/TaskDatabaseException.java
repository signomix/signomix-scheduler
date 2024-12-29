package com.signomix.scheduler.app.ports.driven;

public class TaskDatabaseException extends Exception {

    public static final String DUPLICATE_TASK_ID = "Task ID already exists";

    public TaskDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

}
