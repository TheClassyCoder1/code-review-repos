package com.example.lending.loan.service;

import com.example.lending.loan.client.RiskClient;
import com.example.lending.loan.dto.LoanApplicationRequest;
import com.example.lending.loan.dto.LoanDto;
import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.kafka.LoanAppliedEvent;
import com.example.lending.loan.kafka.LoanEventProducer;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final RiskClient riskClient;
    private final LoanEventProducer eventProducer;

    public LoanService(LoanRepository loanRepository,
                       RiskClient riskClient,
                       LoanEventProducer eventProducer) {
        this.loanRepository = loanRepository;
        this.riskClient = riskClient;
        this.eventProducer = eventProducer;
    }

    /** Persist loan, publish loan.applied (both brokers), synchronously assess via Feign. */
    public RiskAssessmentDto applyLoan(LoanApplicationRequest request) {
        // An application without a user would be persisted and published as an orphan loan.
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("loan application is missing a userId");
        }
        // A missing or negative amount binds to <= 0 and would be saved and published on loan.applied.
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("loan amount must be positive");
        }
        Loan loan = new Loan();
        loan.setUserId(request.getUserId());
        loan.setAmount(request.getAmount());
        // Clients that omit tier would store a null tier and send it to risk-service; use its default tier.
        loan.setTier(request.getTier() != null ? request.getTier() : "STANDARD");
        loan.setStatus("APPLIED");
        Loan saved = loanRepository.save(loan);

        eventProducer.publishLoanApplied(
                new LoanAppliedEvent(saved.getId(), saved.getUserId(), saved.getAmount()));

        LoanDto dto = new LoanDto(saved.getId(), saved.getAmount(), saved.getTier(), saved.getUserId());
        return riskClient.assess(dto);
    }

    public Loan getLoan(Long id) {
        return loanRepository.findById(id).orElse(null);
    }
}
