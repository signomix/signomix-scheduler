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

import com.signomix.common.Organization;
import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.OrganizationDaoIface;
import com.signomix.common.tsdb.OrganizationDao;
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
    @DataSource("oltp")
    AgroalDataSource oltpDs;

    ForAccessTasksDatabase jobDatabase;
    OrganizationDaoIface organizationDao;

    private static final Long DEFAULT_ORGANIZATION_ID = 1L;

    void onStart(@Observes StartupEvent event) throws SchedulerException, TaskDatabaseException {
        jobDatabase = new TaskDatabase();
        logger.info("The application is starting...");
        jobDatabase.setDataSource(oltpDs);
        //jobDatabase.createDatabase(); // database tables are created by signomix-ta-core microservice
        // stop all running tasks if any
        Set<JobKey> keys = quartz.getJobKeys(null);
        for (JobKey key : keys) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting task: " + key);
            }
            try {
                quartz.deleteJob(key);
            } catch (SchedulerException e) {
                logger.error("Error deleting task: " + key);
            }
        }
        organizationDao = new OrganizationDao();
        organizationDao.setDatasource(oltpDs);

        // create and schedule system tasks
        createSystemTasks();
        jobDatabase.getTasks().forEach(task ->

        {
            scheduleTask(task);
        });
        keys = quartz.getJobKeys(null);
        if (logger.isDebugEnabled()) {
            for (JobKey key : keys) {
                logger.debug("Task: " + key + " is scheduled.");
            }
        }
    }

    public void scheduleTask(TaskDefinition definition) {
        if (!definition.enabled) {
            if(logger.isDebugEnabled()) {
                logger.debug("Task: " + definition.getJobName() + " is disabled.");
            }
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
            if(logger.isDebugEnabled()) {
                logger.debug("Task: " + task.id + " " + task.userId + " user id/type: " + user.uid + "/" + user.type);
            }   
            boolean access = false;
            if (user.type == User.OWNER || (task.userId != null && task.userId.equals(user.uid))) {
                access = true;
            }
            if (task.organization != null && task.organization.intValue() == user.organization.intValue()) {
                access = true;
            }
            if (!access) {
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
        Integer organization = null;
        try {
            if (user.type == User.OWNER) {
                return jobDatabase.getTasks();
            } else if (user.organization != null && user.organization.intValue() != DEFAULT_ORGANIZATION_ID) {
                organization = user.organization.intValue();
                return jobDatabase.getUserTasks(user.uid, organization);
            }
        } catch (TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TaskDefinition createTask(TaskDefinition task, User user) {
        Organization org=null;
        try {
            org = organizationDao.getOrganization(user.organization);
        } catch (IotDatabaseException e) {
            logger.warn("Error getting organization: " + user.organization);
        }
        if (org == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No organization found for user: " + user.uid);
            }
            return null;
        }
        if (org.locked) {
            if (logger.isDebugEnabled()) {
                logger.debug("Organization is locked: " + org.name);
            }
            return null;
        }
        if (user.type != User.OWNER) {
            // only system owner can modify task
            if (logger.isDebugEnabled()) {
                logger.debug("No authorization : " + task.id + " user id/type: " + user.uid + "/" + user.type);
            }
            return null;
        }
        if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
            // only system owner or task owner can access task
            return null;
        }
        try {
            task.userId = user.uid;
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Creating task: " + task.description + " user id/type: " + user.uid + "/" + user.type
                                + " task id: "
                                + task.id);
            }
            task.id = null;
            task.id = jobDatabase.addTask(task);
            scheduleTask(task);
            return task;
        } catch (

        TaskDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TaskDefinition updateTask(TaskDefinition task, User user) {
        Organization org=null;
        try {
            org = organizationDao.getOrganization(user.organization);
        } catch (IotDatabaseException e) {
            logger.warn("Error getting organization: " + user.organization);
        }
        if (org == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No organization found for user: " + user.uid);
            }
            return null;
        }
        if (org.locked) {
            if (logger.isDebugEnabled()) {
                logger.debug("Organization is locked: " + org.name);
            }
            return null;
        }
        try {
            TaskDefinition oldTask = jobDatabase.getTask(task.id);
            if (user.type != User.OWNER) {
                // only system owner can modify task
                if (logger.isDebugEnabled()) {
                    logger.debug("No authorization : " + task.id + " user id/type: " + user.uid + "/" + user.type);
                }
                return null;
            }
            if (task.userId != null && !(task.userId.equals(user.uid) || user.type == User.OWNER)) {
                // only system owner or task owner can access task
                if (logger.isDebugEnabled()) {
                    logger.debug("No authorization : " + task.id + " user id/type: " + user.uid + "/" + user.type);
                }
                return null;
            }
            if (task.organization != null) {
                // only system owner or organization admin can modify task
                if (user.type != User.OWNER
                        && task.organization.intValue() != user.organization.intValue()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No authorization : " + task.id + ", organization=" + task.organization);
                    }
                    return null;
                }
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Task: " + definition.getJobName() + " unscheduled.");
                }
            } else {
                logger.error("Task: " + definition.getJobName() + " not unscheduled.");
            }
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void reschedule(JobDataMap dataMap){
        // no need to add "disposable" parameter because Cron definition supports year field
        /*
        Boolean disposable = Boolean.valueOf((String)dataMap.get("disposable"));
            if(!disposable){
                Long id = (Long) dataMap.get("id");
                unscheduleTask(jobDatabase.getTask(id));
            }
        } catch (TaskDatabaseException e) {
            e.printStackTrace();
        } */
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
        try {
            int numberOfTasks = jobDatabase.getTaskCount();
            if (numberOfTasks > 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Event tasks
        TaskDefinition task;

        task = new TaskDefinition();
        // task.id = 1L;
        task.type = TaskDefinition.EVENT;
        task.enabled = true;
        task.description = "System monitor";
        // task.jobName = "system-monitor";
        // task.jobGroup = "events";
        task.scheduleDefinition = "0 0/2 * * * ?"; // Every 2 minutes
        task.nlScheduleDefinition = "Every 2 minutes";
        // task.triggerName = "monitor-trigger";
        // task.triggerGroup = "events";
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
        // task.id = 2L;
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
        // task.id = 3L;
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
        // task.id = 4L;
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
        // task.id = 5L;
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
        // task.id = 6L;
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
        // task.id = 7L;
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
        // task.id = 8L;
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
        // task.id = 9L;
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

        // system command job
        task = new TaskDefinition();
        // task.id = 10L;
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
