package com.signomix.scheduler.app.ports.driven;

import java.util.List;

import com.signomix.scheduler.dto.TaskDefinition;

public interface ForAccessTasksDatabase {

    public List<TaskDefinition> getTasks();
    
}
