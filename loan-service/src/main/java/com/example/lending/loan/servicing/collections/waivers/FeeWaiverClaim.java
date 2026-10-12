package com.example.lending.loan.servicing.collections.waivers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

/** One waiver claimed by a borrower. */
@Entity
@Table(name = "servicing_fee_waiver_claims", schema = "lending")
public class FeeWaiverClaim {

    public static final int GET_TYPE_SELF_SERVICE = 1;
    public static final int USE_STATUS_UNUSED = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;
    @Column(name = "claim_code", nullable = false)
    private String claimCode;
    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;
    @Column(name = "create_time", nullable = false)
    private Date createTime;
    @Column(name = "get_type")
    private Integer getType;
    @Column(name = "use_status")
    private Integer useStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getClaimCode() { return claimCode; }
    public void setClaimCode(String claimCode) { this.claimCode = claimCode; }
    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getGetType() { return getType; }
    public void setGetType(Integer getType) { this.getType = getType; }
    public Integer getUseStatus() { return useStatus; }
    public void setUseStatus(Integer useStatus) { this.useStatus = useStatus; }
}
