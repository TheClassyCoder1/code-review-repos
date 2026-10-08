package com.example.lending.loan.document.export;

public class LoanExportParams {

    private String status;
    private boolean download;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDownload() { return download; }
    public void setDownload(boolean download) { this.download = download; }
}
