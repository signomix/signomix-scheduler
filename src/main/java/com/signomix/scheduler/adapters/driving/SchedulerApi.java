package com.signomix.scheduler.adapters.driving;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.scheduler.app.logic.TaskRunner;
import com.signomix.scheduler.app.ports.driving.AuthPort;
import com.signomix.scheduler.app.ports.driving.ForScheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/scheduler")
public class SchedulerApi {

    @Inject
    Logger logger;

    @Inject
    AuthPort authPort;

    ForScheduler schedulerPort = new TaskRunner();

    @POST
    @Path("/reload")
    public Response restart(@HeaderParam("Authentication") String token) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if(user.type!=User.OWNER){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        schedulerPort.reloadSystemTasks(user);
        return Response.ok().build();
    }

    @POST
    @Path("/restart/{taskId}")
    public Response restartTask(@HeaderParam("Authentication") String token, @PathParam("taskId") Long taskId) {
        User user = authPort.getUser(token);
        schedulerPort.reloadTask(taskId, user);
        return Response.ok().build();
    }

}
