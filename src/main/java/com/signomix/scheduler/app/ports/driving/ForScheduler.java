package com.signomix.scheduler.app.ports.driving;

import com.signomix.common.User;

public interface ForScheduler {

    public void reloadSystemTasks(User user);

    public void reloadTask(long taskId, User user);

}
