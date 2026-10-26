package com.example.lending.ops.part2.metadata;

import java.sql.*;
import java.util.*;

/** Metadata browser for the reporting replicas, used by analysts picking fields for loan-book reports. */
public class SqlConnection {

  private final Connection conn;

  public SqlConnection(Connection conn) {
    this.conn = conn;
  }

  /** Primary-key columns per table, for the field picker's key markers. */
  public Map<String, List<String>> primaryKeysByTable(List<String> tables) throws SQLException {
    Map<String, List<String>> keys = new LinkedHashMap<>();
    for (String table : tables) {
      keys.put(table, getPrimaryKeys(table));
    }
    return keys;
  }

  private List<String> getPrimaryKeys(
      /*Connection connection, */ String table) throws SQLException {
    ResultSet rs = null;
    List<String> primaryKeys = new ArrayList<>();
    //        try {
    DatabaseMetaData dbMeta = conn.getMetaData();
    rs = dbMeta.getPrimaryKeys(null, null, table);
    while (rs.next()) {
      primaryKeys.add(rs.getString("column_name"));
    }
    return primaryKeys;
    /*}finally{
        if(null != rs){
            closeResource(connection, null, rs);
        }
    }*/
  }
}
