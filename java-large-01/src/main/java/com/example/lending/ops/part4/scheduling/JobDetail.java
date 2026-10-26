package com.example.lending.ops.part4.scheduling;

import java.util.Map;
import java.util.function.Supplier;

/** A job definition: its name, how to instantiate it, and its fixed (non-null) job data. */
public record JobDetail(String name, Supplier<? extends Job> factory, Map<String, Object> jobData) {
    public JobDetail {
        jobData = Map.copyOf(jobData);
    }
}
