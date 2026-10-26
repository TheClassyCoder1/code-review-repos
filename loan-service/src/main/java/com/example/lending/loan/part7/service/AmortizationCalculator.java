package com.example.lending.loan.part7.service;

import com.example.lending.loan.part7.entity.RepaymentInstallment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Level-payment (French) amortization. All money is BigDecimal at scale 2; the last
 * installment absorbs the rounding remainder so the principal column sums exactly to the balance.
 */
@Component
public class AmortizationCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    public List<RepaymentInstallment> build(Long loanId, BigDecimal principal, double annualRatePercent,
                                            int termMonths, LocalDate firstDueDate) {
        BigDecimal monthlyRate = monthlyRate(annualRatePercent);
        BigDecimal payment = levelPayment(principal, monthlyRate, termMonths);

        List<RepaymentInstallment> installments = new ArrayList<>(termMonths);
        BigDecimal balance = principal.setScale(2, RoundingMode.HALF_EVEN);
        for (int n = 1; n <= termMonths; n++) {
            BigDecimal interest = balance.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal principalPart = n == termMonths
                    ? balance
                    : payment.subtract(interest).min(balance);
            balance = balance.subtract(principalPart);
            installments.add(new RepaymentInstallment(loanId, n, firstDueDate.plusMonths(n - 1L),
                    principalPart, interest, balance));
        }
        return installments;
    }

    public BigDecimal levelPayment(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN);
        }
        // P * r / (1 - (1 + r)^-n)
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(termMonths, MC);
        BigDecimal discount = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, MC));
        return principal.multiply(monthlyRate, MC).divide(discount, MC).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal monthlyRate(double annualRatePercent) {
        return BigDecimal.valueOf(annualRatePercent).divide(TWELVE_HUNDRED, MC);
    }

    /** Simple daily accrual on the outstanding principal since the last paid due date. */
    public BigDecimal accruedInterest(BigDecimal outstanding, double annualRatePercent,
                                      LocalDate since, LocalDate until) {
        long days = Math.max(0, ChronoUnit.DAYS.between(since, until));
        if (days == 0 || outstanding.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal dailyRate = BigDecimal.valueOf(annualRatePercent)
                .divide(BigDecimal.valueOf(100), MC)
                .divide(DAYS_IN_YEAR, MC);
        return outstanding.multiply(dailyRate, MC).multiply(BigDecimal.valueOf(days), MC);
    }

    /** Recovers the nominal annual rate implied by an existing schedule's first installment. */
    public double impliedAnnualRate(RepaymentInstallment first) {
        BigDecimal openingBalance = first.getRemainingBalance().add(first.getPrincipal());
        if (openingBalance.signum() == 0) {
            return 0.0;
        }
        return first.getInterest()
                .divide(openingBalance, MC)
                .multiply(TWELVE_HUNDRED, MC)
                .setScale(4, RoundingMode.HALF_EVEN)
                .doubleValue();
    }
}
