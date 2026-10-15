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
        Loan loan = new Loan();
        loan.setUserId(request.getUserId());
        loan.setAmount(request.getAmount());
        loan.setTier(request.getTier());
        loan.setStatus("APPROVED");
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
