package com.example.lending.loan.ops.setup;

import java.util.regex.Pattern;

public class DbConnectionForm {

    public enum DbType {
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%s/%s"),
        ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%s/%s"),
        DB2("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://%s:%s/%s");

        private final String driver;
        private final String urlTemplate;

        DbType(String driver, String urlTemplate) {
            this.driver = driver;
            this.urlTemplate = urlTemplate;
        }
    }

    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9.-]{1,253}$");
    private static final Pattern DB_NAME = Pattern.compile("^[A-Za-z0-9_]{1,63}$");

    private DbType dbType = DbType.POSTGRESQL;
    private String host;
    private int port;
    private String dbName;
    private String login;
    private String password;
    private String url;

    public DbType getDbType() { return dbType; }
    public void setDbType(DbType dbType) { this.dbType = dbType; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriver() {
        return dbType.driver;
    }

    public String getURL() {
        return url;
    }

    public void updateUrl() {
        if (host == null || !HOST.matcher(host).matches() || dbName == null || !DB_NAME.matcher(dbName).matches()
                || port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid host, port or database name");
        }
        this.url = String.format(dbType.urlTemplate, host, port, dbName);
    }
}
