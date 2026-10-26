package com.example.lending.loan.part6.statement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Typed view of part6-statements.properties. Environment overrides use the same keys. */
@Configuration
@PropertySource("classpath:part6-statements.properties")
public class StatementSettings {

    private final int maxRows;
    private final double minimumPaymentPercent;
    private final double minimumPaymentFloor;
    private final int dueAfterDays;
    private final int retentionDays;

    public StatementSettings(
            @Value("${lending.statement.export.row-limit}") int maxRows,
            @Value("${lending.statement.minimum-payment-percent}") double minimumPaymentPercent,
            @Value("${lending.statement.minimum-payment-floor}") double minimumPaymentFloor,
            @Value("${lending.statement.due-after-days:21}") int dueAfterDays,
            // retain-days is the pre-rename key; read as a fallback until all overrides use retention-days
            @Value("${lending.statement.retention-days:${lending.statement.retain-days:400}}") int retentionDays) {
        this.maxRows = maxRows;
        this.minimumPaymentPercent = minimumPaymentPercent;
        this.minimumPaymentFloor = minimumPaymentFloor;
        this.dueAfterDays = dueAfterDays;
        this.retentionDays = retentionDays;
    }

    public int getMaxRows() { return maxRows; }

    public double getMinimumPaymentPercent() { return minimumPaymentPercent; }

    public double getMinimumPaymentFloor() { return minimumPaymentFloor; }

    public int getDueAfterDays() { return dueAfterDays; }

    public int getRetentionDays() { return retentionDays; }
}
