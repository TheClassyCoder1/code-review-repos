package com.example.lending.ops.part4.scheduling;

import java.time.Duration;
import java.util.Map;

public interface Scheduler {

    void scheduleJob(JobDetail jobDetail, Duration interval, Map<String, Object> triggerData) throws Exception;

    void pauseAll() throws Exception;
}
