package com.example.lending.ops.part4.scheduling;

public interface JobExecutionContext {

    /** Job data merged from the job definition and the trigger that fired it; trigger values win. */
    JobDataMap getMergedJobDataMap();
}
