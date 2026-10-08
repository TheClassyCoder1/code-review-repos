package com.example.lending.loan.scheduling;

/** A unit of work the job scheduler can run by name. */
public interface ScheduledTask {

    String name();

    void run();
}
