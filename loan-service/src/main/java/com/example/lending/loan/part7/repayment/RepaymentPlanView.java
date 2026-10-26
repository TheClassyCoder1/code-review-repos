package com.example.lending.loan.part7.repayment;

import java.time.LocalDate;
import java.util.List;

/** Response shape for the v2 repayment-plan endpoints. */
public class RepaymentPlanView {

    private Long planId;
    private Long loanId;
    private double principal;
    private double annualRate;
    private int termMonths;
    private double monthlyPayment;
    private String status;
    private double outstandingPrincipal;
    private LocalDate nextDueDate;
    private List<InstallmentView> installments;

    public static RepaymentPlanView of(RepaymentPlan plan, List<RepaymentInstallment> installments,
                                       double outstandingPrincipal) {
        RepaymentPlanView view = new RepaymentPlanView();
        view.planId = plan.getId();
        view.loanId = plan.getLoanId();
        view.principal = plan.getPrincipal();
        view.annualRate = plan.getAnnualRate();
        view.termMonths = plan.getTermMonths();
        view.monthlyPayment = plan.getMonthlyPayment();
        view.status = plan.getStatus();
        view.outstandingPrincipal = outstandingPrincipal;
        view.nextDueDate = installments.stream()
                .filter(RepaymentInstallment::isOutstanding)
                .map(RepaymentInstallment::getDueDate)
                .findFirst()
                .orElse(null);
        view.installments = installments.stream().map(InstallmentView::of).toList();
        return view;
    }

    public Long getPlanId() { return planId; }
    public Long getLoanId() { return loanId; }
    public double getPrincipal() { return principal; }
    public double getAnnualRate() { return annualRate; }
    public int getTermMonths() { return termMonths; }
    public double getMonthlyPayment() { return monthlyPayment; }
    public String getStatus() { return status; }
    public double getOutstandingPrincipal() { return outstandingPrincipal; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public List<InstallmentView> getInstallments() { return installments; }

    public static class InstallmentView {
        private int sequenceNo;
        private LocalDate dueDate;
        private double principalPart;
        private double interestPart;
        private double totalDue;
        private double remainingBalance;
        private String status;

        static InstallmentView of(RepaymentInstallment installment) {
            InstallmentView view = new InstallmentView();
            view.sequenceNo = installment.getSequenceNo();
            view.dueDate = installment.getDueDate();
            view.principalPart = installment.getPrincipalPart();
            view.interestPart = installment.getInterestPart();
            view.totalDue = installment.getTotalDue();
            view.remainingBalance = installment.getRemainingBalance();
            view.status = installment.getStatus();
            return view;
        }

        public int getSequenceNo() { return sequenceNo; }
        public LocalDate getDueDate() { return dueDate; }
        public double getPrincipalPart() { return principalPart; }
        public double getInterestPart() { return interestPart; }
        public double getTotalDue() { return totalDue; }
        public double getRemainingBalance() { return remainingBalance; }
        public String getStatus() { return status; }
    }
}
