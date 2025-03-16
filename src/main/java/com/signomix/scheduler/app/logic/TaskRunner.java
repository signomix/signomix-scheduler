package com.signomix.scheduler.app.logic;

import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.signomix.common.User;
import com.signomix.scheduler.adapters.driven.TaskDatabase;
import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.app.ports.driven.TaskDatabaseException;
import com.signomix.scheduler.app.ports.driving.ForScheduler;
import com.signomix.scheduler.dto.TaskDefinition;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class TaskRunner implements ForScheduler {

    @Inject
    Logger logger;

    @Inject
    org.quartz.Scheduler quartz;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    ForAccessTasksDatabase jobDatabase;

    void onStart(@Observes StartupEvent event) throws SchedulerException, TaskDatabaseException {
        jobDatabase = new TaskDatabase();
        logger.info("The application is starting...");
        jobDatabase.setDataSource(olapDs);
        jobDatabase.createDatabase();
        // stop all running tasks if any
        Set<JobKey> keys = quartz.getJobKeys(null);
        for (JobKey key : keys) {
            logger.info("Deleting task: " + key);
            try {
                quartz.deleteJob(key);
            } catch (SchedulerException e) {
                logger.error("Error deleting task: " + key);
            }
        }
        // create and schedule system tasks
        createSystemTasks();
        jobDatabase.getTasks().forEach(task -> {
            scheduleTask(task);
        });
        keys = quartz.getJobKeys(null);
        for (JobKey key : keys) {
            logger.info("Task: " + key + " is scheduled.");
        }
    }

    public void scheduleTask(TaskDefinition definition) {
        if (!definition.enabled) {
            logger.info("Task: " + definition.getJobName() + " is disabled.");
            return;
        }
        int definitionType = definition.type;
        logger.info("Scheduling task: " + definition.getJobName() + " with schedule: " + definition.scheduleDefinition);
        JobDataMap jobDataMap = new JobDataMap();
        definition.jobDataMap.forEach((k, v) -> {
            jobDataMap.put(k, v);
        });
        jobDataMap.put("id", definition.id);
        JobBuilder jobBuilder = JobBuilder.newJob(getJobClass(definitionType));
        JobDetail job = jobBuilder
                .withIdentity(definition.getJobName(), definition.getJobGroup())
                .setJobData(jobDataMap)
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(definition.getTriggerName(), definition.getTriggerGroup())
                .startNow()
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(definition.scheduleDefinition))
                .build();
        try {
            quartz.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void reloadSystemTasks(User user) {
        try {
            List<TaskDefinition> tasks = jobDatabase.getTasks();
            for (TaskDefinition task : tasks) {
                unscheduleTask(task);
                scheduleTask(task);
            }
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void reloadTask(long taskId, User user) {
        try {
            TaskDefinition task = jobDatabase.getTask(taskId);
            if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
                // only system owner or task owner can access task
                return;
            }
            unscheduleTask(task);
            scheduleTask(task);
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public TaskDefinition getTask(long taskId, User user) {
        try {
            TaskDefinition task = jobDatabase.getTask(taskId);
            logger.info("Task: " + task.id + " " + task.userId + " user id/type: " + user.uid + "/" + user.type);
            if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
                // only system owner or task owner can access task
                return null;
            }
            return task;
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TaskDefinition> getTasks(User user, Integer offset, Integer limit) {
        try {
            return jobDatabase.getTasks();
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TaskDefinition createTask(TaskDefinition task, User user) {
        if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
            // only system owner or task owner can access task
            return null;
        }
        try {
            task.userId = user.uid;
            jobDatabase.addTask(task);
            scheduleTask(task);
            return task;
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TaskDefinition updateTask(TaskDefinition task, User user) {
        try {
            TaskDefinition oldTask = jobDatabase.getTask(task.id);
            if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
                // only system owner or task owner can access task
                logger.info("No authorization : " + task.id + " " + task.userId + " user id/type: " + user.uid + "/"
                        + user.type);
                return null;
            }
            jobDatabase.updateTask(task);
            unscheduleTask(oldTask);
            scheduleTask(task);
            return task;
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private void unscheduleTask(TaskDefinition definition) {
        try {
            boolean deleted = quartz.deleteJob(
                    JobBuilder.newJob(getJobClass(definition.type))
                            .withIdentity(definition.getJobName(), definition.getJobGroup()).build().getKey());
            if (deleted) {
                logger.info("Task: " + definition.getJobName() + " unscheduled.");
            } else {
                logger.error("Task: " + definition.getJobName() + " not unscheduled.");
            }
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Class getJobClass(int type) {
        switch (type) {
            case TaskDefinition.EVENT:
                return EventJob.class;
            case TaskDefinition.REPORT:
                return ReportJob.class;
            case TaskDefinition.SYS_COMMAND:
                return SystemCommandJob.class;
            default:
                return null;
        }
    }

    private void createSystemTasks() throws TaskDatabaseException {
        try{
            int numberOfTasks = jobDatabase.getTaskCount();
            if(numberOfTasks>0){
                return;
            }
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
        // Event tasks
        TaskDefinition task;

        task = new TaskDefinition();
        //task.id = 1L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "System monitor";
        //task.jobName = "system-monitor";
        //task.jobGroup = "events";
        task.scheduleDefinition = "0 0/2 * * * ?"; // Every 2 minutes
        task.nlScheduleDefinition = "Every 2 minutes";
        //task.triggerName = "monitor-trigger";
        //task.triggerGroup = "events";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "system-monitor");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 2L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "Backup";
        // task.scheduleDefinition = "0 0/5 * * * ?"; // Every 5 minutes
        // every day at 23:45
        task.scheduleDefinition = "0 45 23 * * ?";
        task.nlScheduleDefinition = "Every day at 23:45";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "backup");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 3L;
        task.type = TaskDefinition.EVENT;
        task.enabled = false;
        task.description = "Archive";
        task.scheduleDefinition = "0 0/30 * * * ?"; // Every 10 minutes
        task.nlScheduleDefinition = "Every 30 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "archive");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 4L;
        task.type = TaskDefinition.EVENT;
        task.enabled = false;
        task.description = "Data cleaner";
        task.scheduleDefinition = "0 0/15 * * * ?"; // Every 15 minutes
        task.nlScheduleDefinition = "Every 15 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "datacleaner");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 5L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "Device checker";
        task.scheduleDefinition = "0 0/20 * * * ?"; // Every 20 minutes
        task.nlScheduleDefinition = "Every 20 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "devicechecker");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 6L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "Send waiting device commands";
        task.scheduleDefinition = "0 0/3 * * * ?"; // Every 3 minutes
        task.nlScheduleDefinition = "Every 3 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "devicecommands");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        // Report tasks
        task = new TaskDefinition();
        //task.id = 7L;
        task.enabled = true;
        task.description = "Daily report example";
        task.type = TaskDefinition.REPORT;
        // task.scheduleDefinition = "0 0/5 * * * ?"; // Every 5 minutes
        // every day at 00:15
        // task.scheduleDefinition = "0 15 0 * * ?";
        task.scheduleDefinition = "0 0/30 0 * * ?";
        task.nlScheduleDefinition = "Every day at 00:15";
        task.jobDataMap.put("token", "sgx_7d28a2aa17ebaadf5657c4362843b4de");
        task.jobDataMap.put("email", "g.skorupa@gmail.com");
        task.jobDataMap.put("subject", "Daily report");
        task.jobDataMap.put("attachment", "report.html");
        task.jobDataMap.put("dql", "report DqlReport eui IOTEMULATOR channel temperature,humidity last 10 format html");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        task = new TaskDefinition();
        //task.id = 8L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "Paid device checker";
        task.scheduleDefinition = "0 0/10 * * * ?"; // Every 20 minutes
        task.nlScheduleDefinition = "Every 10 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "devicechecker-paid");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        // temporary solution
        task = new TaskDefinition();
        //task.id = 9L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "Reservation sync";
        task.scheduleDefinition = "0 0/2 * * * ?";
        task.nlScheduleDefinition = "Every 2 minutes";
        task.jobDataMap.put("channel", "commands");
        task.jobDataMap.put("message", "reservationsync");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }

        //system command job
        task = new TaskDefinition();
        //task.id = 10L;
        task.type = TaskDefinition.SYS_COMMAND;
        task.enabled = false;
        task.description = "Example system command";
        task.scheduleDefinition = "0 0/1 * * * ?";
        task.nlScheduleDefinition = "Every 1 minute";
        task.jobDataMap.put("command", "df");
        task.jobDataMap.put("option0", "-h");
        try {
            jobDatabase.addTask(task);
        } catch (TaskDatabaseException e) {
            // Ignore duplicate task ID
            if (!e.getMessage().equals(TaskDatabaseException.DUPLICATE_TASK_ID)) {
                throw e;
            }
        }   


    }

}
