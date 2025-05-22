package com.signomix.scheduler.app.ports.driven;

import java.util.List;

import com.signomix.common.db.IotDatabaseException;
import com.signomix.scheduler.dto.TaskDefinition;

import io.agroal.api.AgroalDataSource;

public interface ForAccessTasksDatabase {

    /**
     * Sets the data source for accessing the tasks database.
     *
     * @param dataSource the AgroalDataSource to be used for database connections
     * @throws TaskDatabaseException if there is an error setting the data source
     */
    public void setDataSource(AgroalDataSource dataSource) throws TaskDatabaseException;
    /**
     * Creates the database.
     * This method is responsible for initializing and setting up the database.
     */
    public void createDatabase() throws TaskDatabaseException;
    public void backupDb() throws TaskDatabaseException;

    /**
     * Retrieves a list of task definitions from the database.
     *
     * @return a list of {@link TaskDefinition} objects representing the tasks.
     */
    public List<TaskDefinition> getTasks() throws TaskDatabaseException;

    /**
     * Retrive a list of user task definitions from the database.
     * @param userid
     * @return a list of {@link TaskDefinition} objects representing the tasks.
     * @throws TaskDatabaseException
     */
    public List<TaskDefinition> getUserTasks(String userid, Integer organization) throws TaskDatabaseException;

    /**
     * Adds a new task to the database.
     *
     * @param task the task definition to be added
     * @return the ID of the newly added task
     */
    public long addTask(TaskDefinition task) throws TaskDatabaseException;
    /**
     * Retrieves the task definition for the specified task ID.
     *
     * @param taskId the ID of the task to retrieve
     * @return the TaskDefinition object corresponding to the specified task ID
     */
    public TaskDefinition getTask(long taskId) throws TaskDatabaseException;
    /**
     * Updates the specified task in the database.
     *
     * @param task the task definition to be updated
     */
    public void updateTask(TaskDefinition task) throws TaskDatabaseException;
    /**
     * Deletes a task from the database.
     *
     * @param taskId the ID of the task to be deleted
     */
    public void deleteTask(long taskId) throws TaskDatabaseException;

    /**
     * Get number of tasks in the database
     */
    public int getTaskCount() throws TaskDatabaseException;
    
}
