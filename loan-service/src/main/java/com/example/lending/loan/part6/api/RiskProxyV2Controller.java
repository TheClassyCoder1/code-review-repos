package com.example.lending.loan.part6.api;

import com.example.lending.loan.client.RiskWebClient;
import com.example.lending.loan.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v2/risk-proxy/{id}: versioned successor of /api/v1/risk-proxy/{id}
 * (RiskProxyController), which keeps serving the original shape for existing clients.
 */
@RestController
@RequestMapping("/api/v2/risk-proxy")
public class RiskProxyV2Controller {

    private static final double APPEAL_MARGIN = 0.05;

    private final RiskWebClient riskWebClient;
    private final double approvalThreshold;

    public RiskProxyV2Controller(RiskWebClient riskWebClient,
                                 @Value("${lending.risk.threshold}") double approvalThreshold) {
        this.riskWebClient = riskWebClient;
        this.approvalThreshold = approvalThreshold;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskProxyV2View> proxyRisk(@PathVariable Long id) {
        RiskAssessmentDto risk = riskWebClient.getRisk(id);
        if (risk == null) {
            return ResponseEntity.notFound().build();
        }
        int scorePercent = (int) Math.round(Math.max(0.0, Math.min(1.0, risk.getScore())) * 100);
        boolean appealable = "REJECT".equalsIgnoreCase(risk.getDecision())
                && Math.abs(risk.getScore() - approvalThreshold) <= APPEAL_MARGIN;
        return ResponseEntity.ok(new RiskProxyV2View(risk.getLoanId(), scorePercent, risk.getDecision(), appealable));
    }
}
