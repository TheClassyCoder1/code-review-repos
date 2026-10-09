package com.example.lending.loan.servicing.reporting.scripts;

/** One statement of a report script with its position in the script. */
public class ReportSqlCommandCall {

    private final int lineStart;
    private final int lineEnd;
    private final ReportSqlCommand command;
    private final String[] operands;
    private final String originSql;

    public ReportSqlCommandCall(
                                int lineStart,
                                int lineEnd,
                                ReportSqlCommand command,
                                String[] operands,
                                String originSql) {
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.command = command;
        this.operands = operands;
        this.originSql = originSql;
    }

    public int getLineStart() { return lineStart; }
    public int getLineEnd() { return lineEnd; }

    public ReportSqlCommand getCommand() { return command; }
    public String[] getOperands() { return operands.clone(); }

    public String getOriginSql() { return originSql; }
}
