package com.example.lending.platform.billing.part6.interest;

import com.example.lending.platform.billing.dto.LoanDto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Daily simple-interest accrual for billing-linked loans. */
@Service
public class InterestAccrualService {

    private static final int DAYS_PER_YEAR = 365;

    private final double annualRate;

    public InterestAccrualService(@Value("${platform.interest.annual-rate:0.089}") double annualRate) {
        this.annualRate = annualRate;
    }

    /** Interest for a single day on the loan's amount, rounded for posting. */
    public double dailyAccrual(LoanDto loan) {
        if (loan.getAmount() <= 0) {
            return 0.0;
        }
        return InterestRounding.round(loan.getAmount() * annualRate / DAYS_PER_YEAR);
    }

    /** Interest between two days (end exclusive); each day is rounded and posted separately. */
    public double accrualBetween(LoanDto loan, LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days <= 0) {
            return 0.0;
        }
        double daily = dailyAccrual(loan);
        return InterestRounding.round(daily * days);
    }
}
