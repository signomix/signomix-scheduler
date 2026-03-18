package com.signomix.scheduler.adapters.driven;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/reports")
@RegisterRestClient
public interface ReportClient {
    @GET
    @Path("/single")
    Response getSingleReport(
        @HeaderParam("Authentication") String token,
        @QueryParam("query") String query,
        @QueryParam("header") Boolean header
    );

    @GET
    @Path("/page/{dashboardId}")
    Response getDashboardReport(
        @HeaderParam("Authentication") String token,
        @PathParam("dashboardId") String dashboardId,
        @QueryParam("header") Boolean withHeader,
        @QueryParam("title") Boolean withTitle
    );
}
