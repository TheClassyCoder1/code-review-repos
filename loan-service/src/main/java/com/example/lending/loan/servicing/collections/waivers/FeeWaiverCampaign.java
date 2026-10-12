package com.example.lending.loan.servicing.collections.waivers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

/** Limited late-fee waiver offer that borrowers in early arrears can claim from the portal. */
@Entity
@Table(name = "servicing_fee_waiver_campaigns", schema = "lending")
public class FeeWaiverCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "count", nullable = false)
    private Integer count;
    @Column(name = "per_limit", nullable = false)
    private Integer perLimit;
    @Column(name = "receive_count")
    private Integer receiveCount;
    @Column(name = "enable_time", nullable = false)
    private Date enableTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Integer getPerLimit() { return perLimit; }
    public void setPerLimit(Integer perLimit) { this.perLimit = perLimit; }
    public Integer getReceiveCount() { return receiveCount; }
    public void setReceiveCount(Integer receiveCount) { this.receiveCount = receiveCount; }
    public Date getEnableTime() { return enableTime; }
    public void setEnableTime(Date enableTime) { this.enableTime = enableTime; }
}
