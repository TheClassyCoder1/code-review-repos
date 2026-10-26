package com.example.lending.risk.part7.watchlist;

/** Body for POST /api/v2/risk/watchlist. */
public class WatchlistRequest {

    private Long loanId;
    private WatchlistEntry.Reason reason;
    private String analystId;
    private String note;

    public WatchlistRequest() {
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public WatchlistEntry.Reason getReason() { return reason; }
    public void setReason(WatchlistEntry.Reason reason) { this.reason = reason; }

    public String getAnalystId() { return analystId; }
    public void setAnalystId(String analystId) { this.analystId = analystId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
