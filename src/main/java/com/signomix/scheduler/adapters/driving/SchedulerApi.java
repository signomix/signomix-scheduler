package com.signomix.scheduler.adapters.driving;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.scheduler.app.ports.driving.AuthPort;
import com.signomix.scheduler.app.ports.driving.ForScheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/scheduler")
public class SchedulerApi {

    @Inject
    Logger logger;

    @Inject
    AuthPort authPort;

    @Inject
    ForScheduler schedulerPort;

    @GET
    @Path("/tasks")
    public Response getTasks(@HeaderParam("Authentication") String token, 
    @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(schedulerPort.getTasks(user,offset,limit)).build();
    }

    @GET
    @Path("/task/{taskId}")
    public Response getTask(@HeaderParam("Authentication") String token, @PathParam("taskId") Long taskId) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(schedulerPort.getTask(taskId, user)).build();
    }

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
