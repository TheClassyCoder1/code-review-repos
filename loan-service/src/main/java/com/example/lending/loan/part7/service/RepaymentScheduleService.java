package com.example.lending.loan.part7.service;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.part7.dto.RepaymentInstallmentDto;
import com.example.lending.loan.part7.dto.RepaymentScheduleDto;
import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Builds a fixed-payment amortization schedule for an existing loan. The last
 * installment absorbs the rounding remainder so the balance closes at exactly 0.
 */
@Service
public class RepaymentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentScheduleService.class);
    private static final int MAX_TERM_MONTHS = 360;

    private final LoanRepository loanRepository;
    private final TierRatePolicy ratePolicy;

    public RepaymentScheduleService(LoanRepository loanRepository, TierRatePolicy ratePolicy) {
        this.loanRepository = loanRepository;
        this.ratePolicy = ratePolicy;
    }

    public RepaymentScheduleDto scheduleFor(Long loanId, Integer termMonths, LocalDate firstDueDate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " not found"));
        int term = termMonths == null ? ratePolicy.defaultTermMonths(loan.getTier()) : termMonths;
        if (term < 1 || term > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("termMonths must be between 1 and " + MAX_TERM_MONTHS);
        }
        LocalDate start = firstDueDate == null ? LocalDate.now().plusMonths(1).withDayOfMonth(1) : firstDueDate;
        double rate = ratePolicy.annualRate(loan.getTier());
        RepaymentScheduleDto schedule = build(loan, rate, term, start);
        log.debug("schedule built loanId={} term={} installments={}",
                loanId, term, schedule.getInstallments().size());
        return schedule;
    }

    RepaymentScheduleDto build(Loan loan, double annualRate, int term, LocalDate start) {
        BigDecimal balance = money(loan.getAmount());
        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate)
                .divide(BigDecimal.valueOf(12), 12, RoundingMode.HALF_EVEN);
        BigDecimal payment = money(level(loan.getAmount(), monthlyRate.doubleValue(), term));

        List<RepaymentInstallmentDto> rows = new ArrayList<>(term);
        BigDecimal totalInterest = BigDecimal.ZERO;
        for (int n = 1; n <= term; n++) {
            BigDecimal interest = money(balance.multiply(monthlyRate));
            BigDecimal principalPart = payment.subtract(interest);
            if (n == term || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
            }
            BigDecimal installment = principalPart.add(interest);
            balance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            rows.add(new RepaymentInstallmentDto(n, start.plusMonths(n - 1L),
                    installment.doubleValue(), principalPart.doubleValue(),
                    interest.doubleValue(), balance.doubleValue()));
        }
        return new RepaymentScheduleDto(loan.getId(), ratePolicy.normalize(loan.getTier()),
                money(loan.getAmount()).doubleValue(), annualRate, term,
                totalInterest.doubleValue(), rows);
    }

    /** Standard annuity formula; zero-rate loans split evenly. */
    static double level(double principal, double monthlyRate, int term) {
        if (monthlyRate == 0.0) {
            return principal / term;
        }
        double factor = Math.pow(1 + monthlyRate, term);
        return principal * monthlyRate * factor / (factor - 1);
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
