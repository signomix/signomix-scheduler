package com.signomix.scheduler.adapters.driven;

import com.signomix.scheduler.app.ports.driven.ForAccessTasksDatabase;
import com.signomix.scheduler.dto.TaskDefinition;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Dependent
public class TaskDatabase implements ForAccessTasksDatabase {

    private AgroalDataSource datasource;

    @Override
    public void setDataSource(AgroalDataSource dataSource) {
        this.datasource = dataSource;
    }

    @Override
    public void createDatabase() {
        // Table task_definition holds TaskDefinition objects
        String query = """
            CREATE TABLE IF NOT EXISTS task_definition (
                id BIGSERIAL PRIMARY KEY,
                type INT NOT NULL,
                userid VARCHAR,
                enabled BOOLEAN NOT NULL,
                nl_schedule_definition VARCHAR(1024),
                schedule_definition VARCHAR(255),
                description VARCHAR(255),
                organization INT
            )
                """;

        try (
            Connection connection = datasource.getConnection();
            var statement = connection.createStatement()
        ) {
            statement.execute(query);
        } catch (Exception e) {
            throw new RuntimeException(
                "Error creating table task_definition",
                e
            );
        }

        // Table task_parameter holds the parameters for the tasks
        String query2 = """
            CREATE TABLE IF NOT EXISTS task_parameter (
                task_id BIGINT NOT NULL,
                name VARCHAR(32) NOT NULL,
                value VARCHAR(1024) NOT NULL
            )
                """;

        try (
            Connection connection = datasource.getConnection();
            var statement = connection.createStatement()
        ) {
            statement.execute(query2);
        } catch (Exception e) {
            throw new RuntimeException(
                "Error creating table task_parameter",
                e
            );
        }
    }

    @Override
    public void backupDb() {
        String query =
            "COPY task_definition to '/var/lib/postgresql/data/export/task_definition.csv' DELIMITER ';' CSV HEADER;" +
            "COPY task_parameter to '/var/lib/postgresql/data/export/task_parameter.csv' DELIMITER ';' CSV HEADER;";
        try (
            Connection conn = datasource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
        ) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Backup exception", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restoreDb() {
        String query =
            "COPY task_definition FROM '/var/lib/postgresql/data/import/task_definition.csv' DELIMITER ';' CSV HEADER;" +
            "COPY task_parameter FROM '/var/lib/postgresql/data/import/task_parameter.csv' DELIMITER ';' CSV HEADER;";
        try (
            Connection conn = datasource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
        ) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Restore exception", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TaskDefinition> getTasks() {
        ArrayList<TaskDefinition> definitions = new ArrayList<>();
        String query = "SELECT * FROM task_definition";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(query)
        ) {
            while (resultSet.next()) {
                TaskDefinition definition = new TaskDefinition();
                definition.id = resultSet.getLong("id");
                definition.type = resultSet.getInt("type");
                definition.enabled = resultSet.getBoolean("enabled");
                definition.userId = resultSet.getString("userid");
                definition.nlScheduleDefinition = resultSet.getString(
                    "nl_schedule_definition"
                );
                definition.scheduleDefinition = resultSet.getString(
                    "schedule_definition"
                );
                definition.jobDataMap = getTaskParameters(definition.id);
                definition.description = resultSet.getString("description");
                definition.organization = resultSet.getInt("organization");
                if (resultSet.wasNull()) {
                    definition.organization = null;
                }
                definitions.add(definition);
            }
            return definitions;
        } catch (Exception e) {
            throw new RuntimeException("Error getting tasks", e);
        }
    }

    @Override
    public List<TaskDefinition> getUserTasks(
        String userId,
        Integer organization
    ) {
        ArrayList<TaskDefinition> definitions = new ArrayList<>();
        String query;

        if (organization != null) {
            query =
                "SELECT * FROM task_definition WHERE userid = ? OR organization = ?";
        } else {
            query = "SELECT * FROM task_definition WHERE userid = ?";
        }

        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, userId);
            if (organization != null) {
                statement.setInt(2, organization);
            }
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TaskDefinition definition = new TaskDefinition();
                    definition.id = resultSet.getLong("id");
                    definition.type = resultSet.getInt("type");
                    definition.enabled = resultSet.getBoolean("enabled");
                    definition.userId = resultSet.getString("userid");
                    definition.nlScheduleDefinition = resultSet.getString(
                        "nl_schedule_definition"
                    );
                    definition.scheduleDefinition = resultSet.getString(
                        "schedule_definition"
                    );
                    definition.description = resultSet.getString("description");
                    definition.jobDataMap = getTaskParameters(definition.id);
                    definition.organization = resultSet.getInt("organization");
                    if (resultSet.wasNull()) {
                        definition.organization = null;
                    }
                    definitions.add(definition);
                }
            }
            return definitions;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error getting tasks", e);
        }
    }

    @Override
    public long addTask(TaskDefinition task) {
        long taskId = -1;
        String query;
        if (task.id != null) {
            taskId = task.id;
            query = """
                INSERT INTO task_definition (id, type, enabled, userid, nl_schedule_definition, schedule_definition,description, organization)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                type = EXCLUDED.type, enabled = EXCLUDED.enabled, userid = EXCLUDED.userid, nl_schedule_definition = EXCLUDED.nl_schedule_definition, schedule_definition = EXCLUDED.schedule_definition, description = EXCLUDED.description, organization = EXCLUDED.organization
                RETURNING id
                """;
        } else {
            query = """
                INSERT INTO task_definition (type, enabled, userid, nl_schedule_definition, schedule_definition, description, organization)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        }
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            if (task.id != null) {
                statement.setLong(1, task.id);
                statement.setInt(2, task.type);
                statement.setBoolean(3, task.enabled);
                statement.setString(4, task.userId);
                statement.setString(5, task.nlScheduleDefinition);
                statement.setString(6, task.scheduleDefinition);
                statement.setString(7, task.description);
                if (task.organization == null) {
                    statement.setNull(8, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(8, task.organization);
                }
            } else {
                statement.setInt(1, task.type);
                statement.setBoolean(2, task.enabled);
                statement.setString(3, task.userId);
                statement.setString(4, task.nlScheduleDefinition);
                statement.setString(5, task.scheduleDefinition);
                statement.setString(6, task.description);
                if (task.organization == null) {
                    statement.setNull(7, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(7, task.organization);
                }
            }
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    taskId = resultSet.getLong(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding task", e);
        }
        if (taskId != -1) {
            addTaskParameters(taskId, task);
        }
        return taskId;
    }

    @Override
    public TaskDefinition getTask(long taskId) {
        TaskDefinition definition;
        String query = "SELECT * FROM task_definition WHERE id = ?";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setLong(1, taskId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    definition = new TaskDefinition();
                    definition.id = resultSet.getLong("id");
                    definition.type = resultSet.getInt("type");
                    definition.enabled = resultSet.getBoolean("enabled");
                    definition.userId = resultSet.getString("userid");
                    definition.nlScheduleDefinition = resultSet.getString(
                        "nl_schedule_definition"
                    );
                    definition.scheduleDefinition = resultSet.getString(
                        "schedule_definition"
                    );
                    definition.description = resultSet.getString("description");
                    definition.organization = resultSet.getInt("organization");
                    if (resultSet.wasNull()) {
                        definition.organization = null;
                    }
                    definition.jobDataMap = getTaskParameters(taskId);
                    return definition;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting task", e);
        }
    }

    @Override
    public void updateTask(TaskDefinition task) {
        String query = """
            UPDATE task_definition
            SET type = ?, enabled = ?, userid = ?,nl_schedule_definition = ?, schedule_definition = ?, description = ?, organization=?
            WHERE id = ?
            """;
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setInt(1, task.type);
            statement.setBoolean(2, task.enabled);
            statement.setString(3, task.userId);
            statement.setString(4, task.nlScheduleDefinition);
            statement.setString(5, task.scheduleDefinition);
            statement.setString(6, task.description);
            if (task.organization == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, task.organization);
            }
            statement.setLong(8, task.id);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating task", e);
        }
        addTaskParameters(task.id, task);
    }

    @Override
    public void deleteTask(long taskId) {
        String query = "DELETE FROM task_definition WHERE id = ?";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setLong(1, taskId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting task", e);
        }
        deleteTaskParameters(taskId);
    }

    private void addTaskParameters(long taskId, TaskDefinition task) {
        String query = "DELETE FROM task_parameter WHERE task_id = ?";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setLong(1, taskId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting task parameters", e);
        }

        query =
            "INSERT INTO task_parameter (task_id, name, value) VALUES (?, ?, ?)";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            for (var entry : task.jobDataMap.entrySet()) {
                statement.setLong(1, taskId);
                statement.setString(2, entry.getKey());
                statement.setString(3, entry.getValue());
                statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding task parameters", e);
        }
    }

    private void deleteTaskParameters(long taskId) {
        String query = "DELETE FROM task_parameter WHERE task_id = ?";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setLong(1, taskId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting task parameters", e);
        }
    }

    private Map<String, String> getTaskParameters(long taskId) {
        String query = "SELECT * FROM task_parameter WHERE task_id = ?";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.prepareStatement(query)
        ) {
            statement.setLong(1, taskId);
            try (var resultSet = statement.executeQuery()) {
                var parameters = new java.util.HashMap<String, String>();
                while (resultSet.next()) {
                    parameters.put(
                        resultSet.getString("name"),
                        resultSet.getString("value")
                    );
                }
                return parameters;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting task parameters", e);
        }
    }

    @Override
    public int getTaskCount() {
        String query = "SELECT COUNT(*) FROM task_definition";
        try (
            Connection connection = datasource.getConnection();
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery(query)
        ) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting task count", e);
        }
    }
}
