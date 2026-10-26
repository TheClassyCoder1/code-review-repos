package com.example.lending.ops.part4.scheduling;

public interface Job {

    void execute(JobExecutionContext context) throws JobExecutionException;
}
