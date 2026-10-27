package com.example.lending.ops.part2.sqoop;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** A warehouse import job that copies a core-banking table into the reporting lake. */
@Data
public class SqoopParameters {
    private String jobName, sourceParams;

    public enum DbType { ORACLE }

    public enum SqoopQueryType {
        FORM, SQL;

        public int getCode() { return ordinal(); }
    }

    @lombok.Value
    public static class SqoopTaskExecutionContext {
        DbType sourcetype;
        String sourceConnectionParams;
    }

    /** Stored datasource settings; the password is kept base64-encoded. */
    @Data
    public static class BaseConnectionParam {
        private String host, database, user, password;
        private int port;
    }

    @Data
    public static class SourceOracleParameter {
        private String srcTable, srcQuerySql, srcColumns;
        private int srcQueryType;
        private List<Property> mapColumnHive = new ArrayList<>(), mapColumnJava = new ArrayList<>();
    }

    @Data
    public static class Property {
        private String prop, value;
    }
}
