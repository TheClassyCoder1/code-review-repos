package com.example.lending.ops.part2.sqoop;

import com.example.lending.ops.part2.sqoop.AbstractSourceGenerator.DataSourceUtils;
import com.example.lending.ops.part2.sqoop.AbstractSourceGenerator.JSONUtils;
import com.example.lending.ops.part2.sqoop.SqoopParameters.BaseConnectionParam;
import com.example.lending.ops.part2.sqoop.SqoopParameters.Property;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SourceOracleParameter;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopQueryType;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopTaskExecutionContext;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks every value that ends up in a sqoop command line before the command is generated. The
 * command runs under {@code bash -c}: values placed outside quotes (table, columns, user, column
 * mappings) must be plain identifiers, and values placed inside double quotes (password, query)
 * must not contain the characters bash still interprets there: {@code " $ ` \}.
 */
public final class SqoopCommandGuard {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_#.]{0,127}");
    private static final Pattern COLUMN_LIST = Pattern.compile("[A-Za-z][A-Za-z0-9_#]*(,[A-Za-z][A-Za-z0-9_#]*)*");
    private static final Pattern DB_USER = Pattern.compile("[A-Za-z][A-Za-z0-9_.@-]{0,63}");
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9.-]{1,253}");
    private static final Pattern TYPE = Pattern.compile("[A-Za-z][A-Za-z0-9_(),]{0,63}");
    private static final Pattern QUOTABLE = Pattern.compile("[^\"$`\\\\\\r\\n]+");

    private SqoopCommandGuard() {
    }

    public static void check(SqoopParameters parameters, SqoopTaskExecutionContext context) {
        require(IDENTIFIER, parameters.getJobName());
        SourceOracleParameter source = JSONUtils.parseObject(parameters.getSourceParams(), SourceOracleParameter.class);
        BaseConnectionParam connection = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                context.getSourcetype(), context.getSourceConnectionParams());
        if (source == null || connection == null) {
            throw new IllegalArgumentException("source params and connection are required");
        }
        require(HOST, connection.getHost());
        require(IDENTIFIER, connection.getDatabase());
        require(DB_USER, connection.getUser());
        require(QUOTABLE, AbstractSourceGenerator.decodePassword(connection.getPassword()));
        if (source.getSrcQueryType() == SqoopQueryType.FORM.getCode()) {
            require(IDENTIFIER, source.getSrcTable());
            if (source.getSrcColumns() != null) {
                require(COLUMN_LIST, source.getSrcColumns());
            }
        } else {
            require(QUOTABLE, source.getSrcQuerySql());
        }
        for (Property p : Stream.concat(source.getMapColumnHive().stream(), source.getMapColumnJava().stream()).toList()) {
            require(IDENTIFIER, p.getProp());
            require(TYPE, p.getValue());
        }
    }

    private static void require(Pattern pattern, String value) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("sqoop job contains a value with unsupported characters");
        }
    }
}
