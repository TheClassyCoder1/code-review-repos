package com.example.lending.ops.part2.sqoop;

import com.example.lending.ops.part2.sqoop.SqoopParameters.BaseConnectionParam;
import com.example.lending.ops.part2.sqoop.SqoopParameters.DbType;
import com.example.lending.ops.part2.sqoop.SqoopParameters.Property;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SourceOracleParameter;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopTaskExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Shared pieces of the sqoop source argument builders. */
public abstract class AbstractSourceGenerator {

    protected static final String SPACE = " ", DOUBLE_QUOTES = "\"";
    protected static final String DB_CONNECT = "--connect", DB_USERNAME = "--username", DB_PWD = "--password";
    protected static final String TABLE = "--table", COLUMNS = "--columns", QUERY = "--query", QUERY_WHERE = "where";
    protected static final String QUERY_CONDITION = "AND \\$CONDITIONS", QUERY_WITHOUT_CONDITION = "WHERE \\$CONDITIONS";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract String generate(SqoopParameters sqoopParameters, SqoopTaskExecutionContext sqoopTaskExecutionContext);

    protected static String decodePassword(String encoded) {
        return encoded == null ? "" : new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    protected void buildColumnMapToHIve(StringBuilder sb, SourceOracleParameter parameter) {
        appendColumnMap(sb, "--map-column-hive", parameter.getMapColumnHive());
    }

    protected void buildColumnMapToJava(StringBuilder sb, SourceOracleParameter parameter) {
        appendColumnMap(sb, "--map-column-java", parameter.getMapColumnJava());
    }

    private static void appendColumnMap(StringBuilder sb, String option, List<Property> mapping) {
        if (!mapping.isEmpty()) {
            sb.append(SPACE).append(option).append(SPACE)
                    .append(mapping.stream().map(p -> p.getProp() + "=" + p.getValue()).collect(Collectors.joining(",")));
        }
    }

    interface JSONUtils {
        ObjectMapper MAPPER = new ObjectMapper();

        static <T> T parseObject(String json, Class<T> type) {
            try {
                return json == null ? null : MAPPER.readValue(json, type);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid json for " + type.getSimpleName(), e);
            }
        }
    }

    interface DataSourceUtils {
        static Object buildConnectionParams(DbType dbType, String json) {
            return dbType == null ? null : JSONUtils.parseObject(json, BaseConnectionParam.class);
        }

        static String getJdbcUrl(DbType dbType, BaseConnectionParam p) {
            return "jdbc:oracle:thin:@//" + p.getHost() + ":" + p.getPort() + "/" + p.getDatabase();
        }
    }

    interface StringUtils {
        static boolean isNotEmpty(CharSequence cs) { return cs != null && cs.length() > 0; }
    }
}
