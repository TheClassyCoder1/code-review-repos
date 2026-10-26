package com.example.lending.ops.part2.quartz;

import lombok.Data;

/** Executes one firing of a scheduled ops job, e.g. "call the dunning-letter endpoint at 06:00". */
public interface ITaskInvok {
    void invokMethod(SysJob sysJob) throws TaskException;

    @Data
    class SysJob { private Long jobId; private String executePath; }

    class TaskException extends Exception {
        public TaskException(String message) { super(message); }
    }
}
