package com.example.lending.loan.reporting;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "report_schedules", schema = "lending",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_schedules_path", columnNames = "path"))
public class ReportSchedule {

    @Id
    private String id;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "cron", nullable = false)
    private String cron;

    @Column(name = "description")
    private String description;

    @Column(name = "date_created", nullable = false)
    private Timestamp dateCreated;

    @Column(name = "date_updated", nullable = false)
    private Timestamp dateUpdated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }
    public void setDateUpdated(Timestamp dateUpdated) { this.dateUpdated = dateUpdated; }
}
