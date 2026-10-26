package com.example.lending.loan.part7.dto;

import java.math.BigDecimal;
import java.util.List;

/** Response of GET /api/v1/loans/{id}/schedule (consumed by portal-bff). */
public class RepaymentScheduleView {

    private Long loanId;
    private int installmentCount;
    private BigDecimal totalPrincipal;
    private BigDecimal totalInterest;
    private BigDecimal outstanding;
    private List<InstallmentView> installments;

    public RepaymentScheduleView() {
    }

    public RepaymentScheduleView(Long loanId, List<InstallmentView> installments) {
        this.loanId = loanId;
        this.installments = installments;
        this.installmentCount = installments.size();
        this.totalPrincipal = installments.stream()
                .map(InstallmentView::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalInterest = installments.stream()
                .map(InstallmentView::getInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.outstanding = installments.stream()
                .filter(i -> "DUE".equals(i.getStatus()))
                .map(InstallmentView::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(int installmentCount) { this.installmentCount = installmentCount; }

    public BigDecimal getTotalPrincipal() { return totalPrincipal; }
    public void setTotalPrincipal(BigDecimal totalPrincipal) { this.totalPrincipal = totalPrincipal; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public BigDecimal getOutstanding() { return outstanding; }
    public void setOutstanding(BigDecimal outstanding) { this.outstanding = outstanding; }

    public List<InstallmentView> getInstallments() { return installments; }
    public void setInstallments(List<InstallmentView> installments) { this.installments = installments; }
}
