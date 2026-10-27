package com.example.lending.ops.part5.datasource;

import com.example.lending.ops.common.StringUtils;

import java.util.Map;

/** SQL Server URLs for the core-banking reporting replicas some partners expose. */
public class SqlServerUrlStrategy extends AbstractJdbcUrlStrategy {

  @Override
  public String getJdbcUrl(String address, Map<String, String> paramsJson, String paramsStr) {
    String databaseName = paramsJson.getOrDefault("databaseName", "");
    String instanceName = paramsJson.getOrDefault("instance", "");
    StringBuilder builder = new StringBuilder("jdbc:sqlserver://");
    if (StringUtils.isNotBlank(address)) builder.append(address);
    if (StringUtils.isNotBlank(instanceName)) builder.append(";instanceName=").append(instanceName);
    if (StringUtils.isNotBlank(databaseName)) builder.append(";databaseName=").append(databaseName);
    if (!paramsStr.isEmpty()) builder.append(getConnectParams(paramsStr));
    return builder.toString();
  }

  @Override
  public String defaultDriver() {
    return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  }
}
