package com.example.lending.loan.borrower;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences", schema = "lending")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false, unique = true)
    private Long borrowerId;

    @Column(name = "email_opt_in", nullable = false)
    private boolean emailOptIn = true;

    @Column(name = "sms_opt_in", nullable = false)
    private boolean smsOptIn;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }

    public boolean isEmailOptIn() { return emailOptIn; }
    public void setEmailOptIn(boolean emailOptIn) { this.emailOptIn = emailOptIn; }

    public boolean isSmsOptIn() { return smsOptIn; }
    public void setSmsOptIn(boolean smsOptIn) { this.smsOptIn = smsOptIn; }

    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
}
