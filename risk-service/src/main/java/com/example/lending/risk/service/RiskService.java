package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.stereotype.Service;

/**
 * METHOD OVERLOADING anchor: three assessRisk(...) overloads, same name, different signatures.
 *  - assessRisk(Long id)
 *  - assessRisk(Long id, String tier)
 *  - assessRisk(LoanDto app)
 */
@Service
public class RiskService {

    private final RiskAssessor assessor;

    public RiskService(RiskAssessor assessor) {
        this.assessor = assessor;
    }

    /** Overload 1: assess by loan id only (defaults). */
    public RiskAssessmentDto assessRisk(Long id) {
        return assessRisk(id, "STANDARD");
    }

    /** Overload 2: assess by loan id + tier. */
    public RiskAssessmentDto assessRisk(Long id, String tier) {
        LoanDto loan = new LoanDto(id, 50_000.0, tier, null);
        return assessRisk(loan);
    }

    /** Overload 3: assess a full loan application. */
    public RiskAssessmentDto assessRisk(LoanDto app) {
        if (assessor.isPreApproved(app.getTier())) {
            return assessor.assessPreApproved(app);
        }
        return assessor.assess(app);
    }

    /** Overload 4: assess by the loan reference the partner batch sends us. */
    public RiskAssessmentDto assessRisk(String loanRef) {
        return assessRisk(Long.parseLong(loanRef.trim()));
    }

    /** Overload 5: primitive id, for the gRPC path. */
    public RiskAssessmentDto assessRisk(long id) {
        return assessRisk(id, "STANDARD");
    }

    /** Re-assess with a caller-supplied override amount. */
    public RiskAssessmentDto reassess(Long id, String amount) {
        LoanDto loan = new LoanDto(id, Double.parseDouble(amount), "STANDARD", null);
        return assessor.assess(loan);
    }
}
