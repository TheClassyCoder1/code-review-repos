package com.example.lending.loan.part6.statement;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Builds the monthly statement for a loan. The statement covers the last complete calendar
 * month; closing it publishes a StatementClosedEvent so reminders can be queued.
 */
@Service
public class StatementService {

    private final LoanRepository loanRepository;
    private final OutstandingBalanceCalculator calculator;
    private final StatementSettings settings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StatementService(LoanRepository loanRepository,
                            OutstandingBalanceCalculator calculator,
                            StatementSettings settings,
                            ApplicationEventPublisher events) {
        this.loanRepository = loanRepository;
        this.calculator = calculator;
        this.settings = settings;
        this.events = events;
        this.clock = Clock.systemUTC();
    }

    /** Latest closed statement, or null when the loan does not exist or is not yet active. */
    public StatementDto latest(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null || "APPLIED".equals(loan.getStatus())) {
            return null;
        }
        YearMonth period = YearMonth.now(clock).minusMonths(1);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        double opening = loan.getAmount();
        double outstanding = calculator.outstandingBalance(loan, start, end);
        double interest = outstanding - opening;

        List<StatementLineDto> lines = new ArrayList<>();
        lines.add(new StatementLineDto(start.toString(), "Opening balance", round(opening)));
        if (interest > 0) {
            lines.add(new StatementLineDto(end.toString(), "Interest for " + period, round(interest)));
        }
        if (lines.size() > settings.getMaxRows()) {
            lines = lines.subList(lines.size() - settings.getMaxRows(), lines.size());
        }

        StatementDto dto = new StatementDto();
        dto.setLoanId(loanId);
        dto.setPeriodStart(start.toString());
        dto.setPeriodEnd(end.toString());
        dto.setOpeningBalance(round(opening));
        dto.setClosingBalance(round(outstanding));
        dto.setMinimumPayment(minimumPayment(outstanding));
        dto.setDueDate(end.plusDays(settings.getDueAfterDays()).toString());
        dto.setLines(lines);

        events.publishEvent(new StatementClosedEvent(
                loanId, dto.getClosingBalance(), dto.getMinimumPayment(), dto.getDueDate()));
        return dto;
    }

    private double minimumPayment(double balance) {
        if (balance <= 0) {
            return 0.0;
        }
        double percentage = balance * settings.getMinimumPaymentPercent() / 100.0;
        return round(Math.min(balance, Math.max(settings.getMinimumPaymentFloor(), percentage)));
    }

    private static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
