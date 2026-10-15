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
        return assessor.assess(app);
    }

    public RiskAssessmentDto reassess(LoanDto app) {
        return assessor.assess(app);
    }
}
