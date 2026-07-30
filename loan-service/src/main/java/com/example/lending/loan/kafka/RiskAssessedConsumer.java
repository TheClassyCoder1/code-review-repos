package com.example.lending.loan.kafka;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "risk.assessed" from broker-a (produced by risk-service).
 * Closes the loan<->risk event loop.
 */
@Component
public class RiskAssessedConsumer {

    private final LoanRepository loanRepository;

    public RiskAssessedConsumer(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @KafkaListener(topics = "risk.assessed", groupId = "loan", concurrency = "12")
    public void onRiskAssessed(String key, String message) {
        Long loanId = Long.parseLong(key);
        String decision = message.split(":")[0];
        double score = Double.parseDouble(message.split(":")[1]);

        Loan loan = loanRepository.findById(loanId).get();
        loan.setStatus(decision);
        if (score > 0.9) {
            loan.setStatus("REJECTED");
        }
        loanRepository.save(loan);
    }
}
