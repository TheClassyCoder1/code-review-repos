package com.example.lending.loan.servicing;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.fees.AmortizationCalculator;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Re-amortizes the unpaid part of a loan over a new term (hardship programme). */
@Service
public class LoanRestructureService {

    private static final int MIN_TERM_MONTHS = 3;
    private static final int MAX_TERM_MONTHS = 120;

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final AmortizationCalculator amortizationCalculator;
    private final BigDecimal annualRate;

    public LoanRestructureService(LoanRepository loanRepository,
                                  InstallmentRepository installmentRepository,
                                  AmortizationCalculator amortizationCalculator,
                                  @Value("${lending.pricing.annual-rate-bps}") int annualRateBps) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.amortizationCalculator = amortizationCalculator;
        this.annualRate = BigDecimal.valueOf(annualRateBps, 4);
    }

    public List<Installment> restructure(Long loanId, int termMonths) {
        if (termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Term must be between 3 and 120 months");
        }
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        List<Installment> open = installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(loanId, Installment.OPEN);
        if (open.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan has no open installments");
        }

        BigDecimal outstanding = open.stream()
                .map(Installment::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate firstDue = open.get(0).getDueDate();
        int firstSequence = open.get(0).getSequence();

        installmentRepository.deleteAll(open);
        List<Installment> schedule = amortizationCalculator
                .schedule(outstanding, annualRate, termMonths, firstDue)
                .stream()
                .map(payment -> Installment.open(loanId, firstSequence + payment.number() - 1, payment))
                .toList();
        installmentRepository.saveAll(schedule);

        loan.setStatus("RESTRUCTURED");
        loanRepository.save(loan);
        return schedule;
    }
}
