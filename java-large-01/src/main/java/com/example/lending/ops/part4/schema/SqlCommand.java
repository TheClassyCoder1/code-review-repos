package com.example.lending.ops.part4.schema;

/** Statement kinds accepted by the reporting SQL runbook executor. */
public enum SqlCommand {
    SELECT, INSERT_INTO, CREATE_VIEW, ALTER_TABLE, SET
}
