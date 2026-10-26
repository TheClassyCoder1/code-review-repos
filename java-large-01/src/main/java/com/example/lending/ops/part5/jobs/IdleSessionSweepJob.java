package com.example.lending.ops.part5.jobs;

import java.time.Duration;

/** Terminates console sessions that have been idle longer than the category's timeout. */
public class IdleSessionSweepJob extends AbstractScheduledJob {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    SessionManager sessionManager;

    Integer category;

     @Override
    protected void init(JobExecutionContext context){
         super.init(context);
        if(sessionManager == null) {
            sessionManager = getParameter("sessionManager",SessionManager.class);
            category = getParameter("category",Integer.class);
        }
    }

    @Override
    protected void run() {
        if (sessionManager != null) {
            sessionManager.terminateIdle(category, IDLE_TIMEOUT);
        }
    }
}
