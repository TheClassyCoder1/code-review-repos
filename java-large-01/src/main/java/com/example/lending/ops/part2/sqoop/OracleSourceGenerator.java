package com.example.lending.ops.part2.sqoop;

import com.example.lending.ops.part2.sqoop.SqoopParameters.BaseConnectionParam;
import com.example.lending.ops.part2.sqoop.SqoopParameters.DbType;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SourceOracleParameter;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopQueryType;
import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopTaskExecutionContext;

/** Builds the {@code sqoop import} source arguments for the Oracle core-banking schema; used by {@link SqoopImportTask}. */
public class OracleSourceGenerator extends AbstractSourceGenerator {

    @Override
    public String generate(SqoopParameters sqoopParameters, SqoopTaskExecutionContext sqoopTaskExecutionContext) {

        StringBuilder oracleSourceSb = new StringBuilder();

        try {
            SourceOracleParameter sourceOracleParameter =
                    JSONUtils.parseObject(sqoopParameters.getSourceParams(), SourceOracleParameter.class);

            if (null == sourceOracleParameter)
                return oracleSourceSb.toString();
            BaseConnectionParam baseDataSource = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                    sqoopTaskExecutionContext.getSourcetype(),
                    sqoopTaskExecutionContext.getSourceConnectionParams());

            if (null == baseDataSource)
                return oracleSourceSb.toString();

            oracleSourceSb.append(SPACE).append(DB_CONNECT)
                    .append(SPACE).append(DOUBLE_QUOTES)
                    .append(DataSourceUtils.getJdbcUrl(DbType.ORACLE, baseDataSource)).append(DOUBLE_QUOTES)
                    .append(SPACE).append(DB_USERNAME)
                    .append(SPACE).append(baseDataSource.getUser())
                    .append(SPACE).append(DB_PWD)
                    .append(SPACE).append(DOUBLE_QUOTES)
                    .append(decodePassword(baseDataSource.getPassword())).append(DOUBLE_QUOTES);

            // sqoop table & sql query
            if (sourceOracleParameter.getSrcQueryType() == SqoopQueryType.FORM.getCode()) {
                if (StringUtils.isNotEmpty(sourceOracleParameter.getSrcTable())) {
                    oracleSourceSb.append(SPACE).append(TABLE)
                            .append(SPACE).append(sourceOracleParameter.getSrcTable());
                }

                if (StringUtils.isNotEmpty(sourceOracleParameter.getSrcColumns())) {
                    oracleSourceSb.append(SPACE).append(COLUMNS)
                            .append(SPACE).append(sourceOracleParameter.getSrcColumns());
                }
            } else if (sourceOracleParameter.getSrcQueryType() == SqoopQueryType.SQL.getCode()
                    && StringUtils.isNotEmpty(sourceOracleParameter.getSrcQuerySql())) {

                String srcQuery = sourceOracleParameter.getSrcQuerySql();
                oracleSourceSb.append(SPACE).append(QUERY)
                        .append(SPACE).append(DOUBLE_QUOTES).append(srcQuery);

                if (srcQuery.toLowerCase().contains(QUERY_WHERE)) {
                    oracleSourceSb.append(SPACE).append(QUERY_CONDITION).append(DOUBLE_QUOTES);
                } else {
                    oracleSourceSb.append(SPACE).append(QUERY_WITHOUT_CONDITION).append(DOUBLE_QUOTES);
                }
            }
            // sqoop hive map column
            buildColumnMapToHIve(oracleSourceSb, sourceOracleParameter);
            // sqoop map column java
            buildColumnMapToJava(oracleSourceSb, sourceOracleParameter);
        } catch (Exception e) {
            logger.error(String.format("Sqoop task oracle source params build failed: [%s]", e.getMessage()));
        }

        return oracleSourceSb.toString();
    }
}
