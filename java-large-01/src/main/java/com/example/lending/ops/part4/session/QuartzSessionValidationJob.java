package com.example.lending.ops.part4.session;

import com.example.lending.ops.part4.scheduling.Job;
import com.example.lending.ops.part4.scheduling.JobDataMap;
import com.example.lending.ops.part4.scheduling.JobExecutionContext;
import com.example.lending.ops.part4.scheduling.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Periodic sweep of expired console sessions. Scheduled by {@link QuartzSessionValidationScheduler}. */
public class QuartzSessionValidationJob implements Job {

    static final String SESSION_MANAGER_KEY = "sessionManager";

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzSessionValidationJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        ValidatingSessionManager sessionManager = (ValidatingSessionManager) jobDataMap.get(SESSION_MANAGER_KEY);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing session validation Quartz job...");
        }

        sessionManager.validateSessions();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Session validation Quartz job complete.");
        }
    }
}
