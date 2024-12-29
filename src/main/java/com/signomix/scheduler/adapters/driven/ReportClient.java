package com.signomix.scheduler.adapters.driven;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/api/reports")
@RegisterRestClient
public interface ReportClient {

    @GET
    @Path("/single")
    Response getSingleReport(@HeaderParam("Authentication") String token, @QueryParam("query") String query);

}
