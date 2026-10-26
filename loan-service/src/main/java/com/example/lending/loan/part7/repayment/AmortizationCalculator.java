package com.example.lending.loan.part7.repayment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard annuity (French) amortization. Amounts are rounded to cents per instalment and the
 * rounding residue is folded into the final instalment so the schedule always sums to the principal.
 */
@Component
public class AmortizationCalculator {

    private static final int SCALE = 2;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    /** Fixed monthly payment for the given principal, nominal annual rate (e.g. 0.085) and term. */
    public double monthlyPayment(double principal, double annualRate, int termMonths) {
        validate(principal, annualRate, termMonths);
        if (annualRate == 0.0) {
            return round(principal / termMonths);
        }
        double r = annualRate / 12.0;
        double factor = Math.pow(1 + r, termMonths);
        return round(principal * r * factor / (factor - 1));
    }

    /** Builds the full schedule. Instalments are not persisted; the caller assigns the plan id. */
    public List<RepaymentInstallment> schedule(double principal, double annualRate, int termMonths,
                                               LocalDate firstDueDate) {
        validate(principal, annualRate, termMonths);
        if (firstDueDate == null) {
            throw new IllegalArgumentException("first due date is required");
        }

        BigDecimal payment = BigDecimal.valueOf(monthlyPayment(principal, annualRate, termMonths));
        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate).divide(TWELVE, 10, RoundingMode.HALF_EVEN);
        BigDecimal balance = BigDecimal.valueOf(principal).setScale(SCALE, RoundingMode.HALF_EVEN);

        List<RepaymentInstallment> installments = new ArrayList<>(termMonths);
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(SCALE, RoundingMode.HALF_EVEN);
            BigDecimal principalPart = payment.subtract(interest);
            if (i == termMonths || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
            }
            balance = balance.subtract(principalPart);

            RepaymentInstallment installment = new RepaymentInstallment();
            installment.setSequenceNo(i);
            installment.setDueDate(firstDueDate.plusMonths(i - 1L));
            installment.setPrincipalPart(principalPart.doubleValue());
            installment.setInterestPart(interest.doubleValue());
            installment.setRemainingBalance(balance.doubleValue());
            installment.setStatus(RepaymentInstallment.STATUS_DUE);
            installments.add(installment);
        }
        return installments;
    }

    /** Interest that would still accrue on the outstanding instalments if the loan ran to term. */
    public double remainingInterest(List<RepaymentInstallment> installments) {
        BigDecimal total = BigDecimal.ZERO;
        for (RepaymentInstallment installment : installments) {
            if (installment.isOutstanding()) {
                total = total.add(BigDecimal.valueOf(installment.getInterestPart()));
            }
        }
        return total.setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** Principal still owed across the outstanding instalments. */
    public double outstandingPrincipal(List<RepaymentInstallment> installments) {
        BigDecimal total = BigDecimal.ZERO;
        for (RepaymentInstallment installment : installments) {
            if (installment.isOutstanding()) {
                total = total.add(BigDecimal.valueOf(installment.getPrincipalPart()));
            }
        }
        return total.setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static void validate(double principal, double annualRate, int termMonths) {
        if (principal <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (annualRate < 0 || annualRate > 1) {
            throw new IllegalArgumentException("annual rate must be between 0 and 1");
        }
        if (termMonths < 1 || termMonths > 360) {
            throw new IllegalArgumentException("term must be between 1 and 360 months");
        }
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_EVEN).doubleValue();
    }
}
