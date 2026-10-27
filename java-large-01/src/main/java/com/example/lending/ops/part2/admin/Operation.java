package com.example.lending.ops.part2.admin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Summary shown in the generated admin API reference. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Operation {
    String summary();

    /** Marks an admin operation for the audit trail; the value is the operation title. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface SysLog { String value(); }
}
