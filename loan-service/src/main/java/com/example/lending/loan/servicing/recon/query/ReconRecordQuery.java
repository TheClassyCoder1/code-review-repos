package com.example.lending.loan.servicing.recon.query;

import com.example.lending.loan.servicing.common.PageRequestParams;

import java.util.Objects;

/** Workbench query of reconciliation records; used as a cache key for result pages. */
public class ReconRecordQuery {

    private String reference;
    private PageRequestParams pageParameter;
    private String accountId;

    public ReconRecordQuery() {
    }

    public ReconRecordQuery(String reference, PageRequestParams pageParameter, String accountId) {
        this.reference = reference;
        this.pageParameter = pageParameter;
        this.accountId = accountId;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public PageRequestParams getPageParameter() { return pageParameter; }
    public void setPageParameter(PageRequestParams pageParameter) { this.pageParameter = pageParameter; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReconRecordQuery)) {
            return false;
        }
        ReconRecordQuery that = (ReconRecordQuery) o;
        return Objects.equals(reference, that.reference) && Objects.equals(pageParameter, that.pageParameter)
                && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, pageParameter, accountId);
    }
}
