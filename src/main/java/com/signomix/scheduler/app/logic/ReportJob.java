package com.signomix.scheduler.app.logic;

import com.signomix.common.db.ReportResult;
import com.signomix.scheduler.adapters.driven.ReportClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ReportJob extends Job implements org.quartz.Job {

    @Inject
    Logger logger;

    @RestClient
    ReportClient reportClient;

    @Override
    public void execute(JobExecutionContext context)
        throws JobExecutionException {
        logger.info("Executing report job " + context.getJobDetail().getKey());
        Long id = context.getMergedJobDataMap().getLong("id");
        reschedule(context.getMergedJobDataMap());
        String token = (String) context.getMergedJobDataMap().get("token");
        if (token == null || token.isEmpty()) {
            logger.error("No token defined for report job " + id);
            return;
        }
        String dql = (String) context.getMergedJobDataMap().get("dql");
        String dashboardId = (String) context
            .getMergedJobDataMap()
            .get("dashboard");
        if (
            (dql == null || dql.isEmpty()) &&
            (dashboardId == null || dashboardId.isEmpty())
        ) {
            logger.error(
                "No DQL query or dashboard ID defined for report job " + id
            );
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
        String attachment = (String) context
            .getMergedJobDataMap()
            .get("attachment");
        logger.info("Executing report job " + id + " with DQL query: " + dql);

        // call the report service
        // html header and footer not applicable for email
        boolean withHeader = ((attachment != null &&
                attachment.endsWith(".html")) ||
            (email == null || email.isEmpty()));
        String reportContent = getReportContent(
            token,
            dql,
            dashboardId,
            attachment,
            email
        );

        if (email != null && !email.isEmpty()) {
            // get HTML head and tail
        }
        // Hotfix: for dashboard report, the report content must be send as HTML attachment
        // because of need to use Bootstrap CSS and JS library.
        if (dashboardId != null && !dashboardId.isEmpty()) {
            attachment = "report.html";
        }
        // send the report by email
        sendEmail(email, subject, reportContent, attachment);
    }

    private String getReportContent(
        String token,
        String dql,
        String dashboardId,
        String attachment,
        String email
    ) {
        Response response;
        boolean isDashboard = false;
        if (dashboardId != null && !dashboardId.isEmpty()) {
            isDashboard = true;
            response = reportClient.getDashboardReport(
                token,
                dashboardId,
                true,
                true
            );
        } else {
            // html header and footer not applicable for email
            boolean withHeader = ((attachment != null &&
                    attachment.endsWith(".html")) ||
                (email == null || email.isEmpty()));
            response = reportClient.getSingleReport(token, dql, withHeader);
        }
        if (response.getStatus() != 200) {
            logger.error(
                "Error calling the report service: " + response.getStatus()
            );
            return null;
        }

        String reportContent = response.readEntity(String.class);
        logger.info("Report content: " + reportContent);
        if (reportContent == null || reportContent.trim().isEmpty()) {
            logger.error("Empty report content");
            return null;
        } else {
            reportContent = reportContent.trim();
        }

        if (!isDashboard) {
            if (reportContent.startsWith("{\"datasets")) {
                // reportContent starting with {"datasets" is a JSON object
                // otherwise it is assumed CSV content
                // TODO: implement JSON report processing
                ReportResult reportResult = ReportResult.parse(reportContent);
                if (reportResult.status != 200) {
                    reportContent =
                        "Report error:\n" + reportResult.errorMessage;
                }
            }
        } else {
            // TODO: if the report content is HTML, extract the body content and use it as the email content, otherwise use the whole content as is
        }
        return reportContent;
    }
}
