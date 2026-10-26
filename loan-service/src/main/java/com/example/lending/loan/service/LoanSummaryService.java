package com.example.lending.loan.service;

import com.example.lending.loan.client.RiskWebClient;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the servicing summary shown to collections agents: the stored loan plus the latest
 * risk assessment, so agents don't have to open two screens.
 */
@Service
public class LoanSummaryService {

    private final LoanRepository loanRepository;
    private final RiskWebClient riskWebClient;

    public LoanSummaryService(LoanRepository loanRepository, RiskWebClient riskWebClient) {
        this.loanRepository = loanRepository;
        this.riskWebClient = riskWebClient;
    }

    public Map<String, Object> summarize(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("unknown loan " + loanId));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("loanId", loan.getId());
        summary.put("userId", loan.getUserId());
        summary.put("tier", loan.getTier());
        summary.put("status", loan.getStatus());
        summary.put("principal", loan.getAmount());
        summary.put("risk", riskWebClient.getRisk(loanId));
        return summary;
    }
}
