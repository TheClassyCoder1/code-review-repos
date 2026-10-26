package com.example.lending.loan.service;

import com.example.lending.loan.client.RiskClient;
import com.example.lending.loan.dto.LoanApplicationRequest;
import com.example.lending.loan.dto.LoanDto;
import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.kafka.LoanAppliedEvent;
import com.example.lending.loan.kafka.LoanEventProducer;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final RiskClient riskClient;
    private final LoanEventProducer eventProducer;
    private final ApplicationEventPublisher applicationEvents;

    public LoanService(LoanRepository loanRepository,
                       RiskClient riskClient,
                       LoanEventProducer eventProducer,
                       ApplicationEventPublisher applicationEvents) {
        this.loanRepository = loanRepository;
        this.riskClient = riskClient;
        this.eventProducer = eventProducer;
        this.applicationEvents = applicationEvents;
    }

    /** Persist loan, publish loan.applied (both brokers), synchronously assess via Feign. */
    public RiskAssessmentDto applyLoan(LoanApplicationRequest request) {
        // An application without a user would be persisted and published as an orphan loan.
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("loan application is missing a userId");
        }
        Loan loan = new Loan();
        loan.setUserId(request.getUserId());
        loan.setAmount(BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_UP));
        loan.setTier(request.getTier());
        loan.setStatus("APPLIED");
        Loan saved = loanRepository.save(loan);

        LoanAppliedEvent applied =
                new LoanAppliedEvent(saved.getId(), saved.getUserId(), saved.getAmount().doubleValue());
        applicationEvents.publishEvent(applied);
        eventProducer.publishLoanApplied(applied);

        LoanDto dto = new LoanDto(saved.getId(), saved.getAmount().doubleValue(), saved.getTier(), saved.getUserId());
        return riskClient.assess(dto);
    }

    public Loan getLoan(Long id) {
        return loanRepository.findById(id).orElse(null);
    }
}
