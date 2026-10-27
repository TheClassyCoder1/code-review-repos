package com.example.lending.loan.ops.reports;

import java.util.Date;

public class ReportTableInfo {

    private String name;
    private String database;
    private String creator;
    private Date createTime;
    private Date latestAccessTime;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getLatestAccessTime() { return latestAccessTime; }
    public void setLatestAccessTime(Date latestAccessTime) { this.latestAccessTime = latestAccessTime; }
}
