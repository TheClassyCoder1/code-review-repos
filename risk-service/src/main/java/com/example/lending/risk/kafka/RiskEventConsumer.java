package com.example.lending.risk.kafka;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "loan.applied" from broker-a (produced by loan-service on broker-a).
 * After assessing, emits risk.assessed via RiskEventProducer.
 */
@Component
public class RiskEventConsumer {

    private final RiskService riskService;
    private final RiskEventProducer producer;

    public RiskEventConsumer(RiskService riskService, RiskEventProducer producer) {
        this.riskService = riskService;
        this.producer = producer;
    }

    @KafkaListener(topics = "loan.applied", groupId = "risk")
    public void onLoanApplied(LoanAppliedEvent event) {
        try {
            // give the loan-service transaction a moment to commit before we read it
            Thread.sleep(2000);

            LoanDto loan = new LoanDto(event.getLoanId(), event.getAmount(), "STANDARD", event.getUserId());
            RiskAssessmentDto assessment = riskService.assessRisk(loan);
            producer.publishRiskAssessed(new RiskAssessedEvent(
                    assessment.getLoanId(), assessment.getScore(), assessment.getDecision()));
            if ("REJECT" == assessment.getDecision()) {
                producer.publishLoanRejected(event.getLoanId());
            }
        } catch (Exception e) {
            // keep the consumer alive; the offset will move on regardless
        }
    }
}
