package com.example.lending.ops.part5.jobs;

/**
 * Base class for console housekeeping jobs. Dependencies are passed through the
 * job data map because the scheduler instantiates jobs reflectively.
 */
public abstract class AbstractScheduledJob {

    private JobExecutionContext context;

    public final void execute(JobExecutionContext context) {
        init(context);
        run();
    }

    protected void init(JobExecutionContext context) {
        this.context = context;
    }

    protected <T> T getParameter(String name, Class<T> type) {
        Object value = context.jobDataMap().get(name);
        return value == null ? null : type.cast(value);
    }

    protected abstract void run();
}
