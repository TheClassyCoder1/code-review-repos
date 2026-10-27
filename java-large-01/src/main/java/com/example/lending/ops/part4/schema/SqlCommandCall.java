package com.example.lending.ops.part4.schema;

/** A parsed statement from a reporting SQL runbook, with the line span it came from. */
public class SqlCommandCall {

    private final int lineStart;
    private final int lineEnd;
    private final SqlCommand command;
    private final String[] operands;
    private final String originSql;

    public SqlCommandCall(
                          int lineStart,
                          int lineEnd,
                          SqlCommand command,
                          String[] operands,
                          String originSql) {
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.command = command;
        this.operands = operands;
        this.originSql = originSql;
    }

    public String describe() { return command + " lines " + lineStart + "-" + lineEnd + ": " + originSql + " " + operands.length; }
}
