package com.signomix.scheduler.app.logic;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.signomix.common.db.ReportResult;
import com.signomix.scheduler.adapters.driven.ReportClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

public class ReportJob extends Job implements org.quartz.Job {

    @Inject
    Logger logger;

    @RestClient
    ReportClient reportClient;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long id = context.getMergedJobDataMap().getLong("id");
        checkAndReschedule(id);
        String token = (String) context.getMergedJobDataMap().get("token");
        if (token == null || token.isEmpty()) {
            logger.error("No token defined for report job " + id);
            return;
        }
        String dql = (String) context.getMergedJobDataMap().get("dql");
        if (dql == null || dql.isEmpty()) {
            logger.error("No DQL query defined for report job " + id);
            return;
        }
        String email = (String) context.getMergedJobDataMap().get("email");
        if (email == null || email.isEmpty()) {
            logger.error("No target email defined for report job");
            return;
        }
        String subject = (String) context.getMergedJobDataMap().get("subject");
        if (subject == null || subject.isEmpty()) {
            subject = "Scheduled report job " + id;
        }
        logger.info("Executing report job " + id + " with DQL query: " + dql);

        // call the report service
        Response response = reportClient.getSingleReport(token, dql);
        if (response.getStatus() != 200) {
            logger.error("Error calling the report service: " + response.getStatus());
            return;
        }
        String reportContent = response.readEntity(String.class);
        logger.info("Report content: " + reportContent);
        if (reportContent == null || reportContent.trim().isEmpty()) {
            logger.error("Empty report content");
            return;
        } else {
            reportContent = reportContent.trim();
        }
        // reportContent starting with {"datasets" is a JSON object
        // otherwise it is assumed CSV content

        if (reportContent.startsWith("{\"datasets")) {
            // TODO: implement JSON report processing
            ReportResult reportResult = ReportResult.parse(reportContent);
            if (reportResult.status != 200) {
                reportContent= "Report error:\n" + reportResult.errorMessage;
            }
        }

        // send the report by email
        sendEmail(email, subject, reportContent);
    }

}
