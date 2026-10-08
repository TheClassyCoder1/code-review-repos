package com.example.lending.loan.servicing;

import com.example.lending.loan.fees.AmortizationCalculator.ScheduledPayment;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installments", schema = "lending")
public class Installment {

    public static final String OPEN = "OPEN";
    public static final String PARTIAL = "PARTIAL";
    public static final String PAID = "PAID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "sequence_no", nullable = false)
    private int sequence;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false)
    private BigDecimal interestDue;

    @Column(name = "principal_paid", nullable = false)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", nullable = false)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version")
    private Long version;

    protected Installment() {
    }

    public static Installment open(Long loanId, int sequence, ScheduledPayment payment) {
        Installment installment = new Installment();
        installment.loanId = loanId;
        installment.sequence = sequence;
        installment.dueDate = payment.dueDate();
        installment.principalDue = payment.principal();
        installment.interestDue = payment.interest();
        installment.status = OPEN;
        return installment;
    }

    public BigDecimal interestOutstanding() {
        return interestDue.subtract(interestPaid);
    }

    public BigDecimal principalOutstanding() {
        return principalDue.subtract(principalPaid);
    }

    public Long getId() { return id; }
    public Long getLoanId() { return loanId; }
    public int getSequence() { return sequence; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getPrincipalDue() { return principalDue; }
    public BigDecimal getInterestDue() { return interestDue; }

    public BigDecimal getPrincipalPaid() { return principalPaid; }
    public void setPrincipalPaid(BigDecimal principalPaid) { this.principalPaid = principalPaid; }

    public BigDecimal getInterestPaid() { return interestPaid; }
    public void setInterestPaid(BigDecimal interestPaid) { this.interestPaid = interestPaid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
