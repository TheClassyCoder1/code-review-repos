package com.example.lending.loan.borrower;

/** Fields a borrower may change on their own notification settings. */
public class PreferenceUpdateRequest {

    private boolean emailOptIn;
    private boolean smsOptIn;
    private boolean marketingOptIn;

    public boolean isEmailOptIn() { return emailOptIn; }
    public void setEmailOptIn(boolean emailOptIn) { this.emailOptIn = emailOptIn; }

    public boolean isSmsOptIn() { return smsOptIn; }
    public void setSmsOptIn(boolean smsOptIn) { this.smsOptIn = smsOptIn; }

    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }
}
