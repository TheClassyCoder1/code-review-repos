package com.example.lending.loan.fees;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PayoffQuoteService {

    private static final int MAX_QUOTE_DAYS = 30;
    private static final int BASIS_POINTS = 10_000;
    private static final int DAYS_PER_YEAR = 365;

    private final LoanRepository loanRepository;
    private final Clock clock;
    private final int annualRateBps;

    public PayoffQuoteService(LoanRepository loanRepository,
                              Clock clock,
                              @Value("${lending.pricing.annual-rate-bps}") int annualRateBps) {
        this.loanRepository = loanRepository;
        this.clock = clock;
        this.annualRateBps = annualRateBps;
    }

    public PayoffQuote quote(Long loanId, LocalDate payoffDate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        LocalDate today = LocalDate.now(clock);
        if (payoffDate.isBefore(today) || payoffDate.isAfter(today.plusDays(MAX_QUOTE_DAYS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payoff date must be within " + MAX_QUOTE_DAYS + " days");
        }

        int principalCents = (int) Math.round(loan.getAmount() * 100);
        int days = (int) ChronoUnit.DAYS.between(today, payoffDate);
        int accruedCents = principalCents * annualRateBps * days / (BASIS_POINTS * DAYS_PER_YEAR);
        return new PayoffQuote(loanId, payoffDate, principalCents, accruedCents, principalCents + accruedCents);
    }

    public record PayoffQuote(Long loanId, LocalDate payoffDate, long principalCents,
                              long accruedInterestCents, long totalCents) {
    }
}
