package com.example.lending.loan.batch;

public enum BatchJobStatus {

    JOB_STATUS_RELEASE("1"),
    JOB_STATUS_RUNNING("2"),
    JOB_STATUS_NOT_RUNNING("3");

    private final String type;

    BatchJobStatus(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
