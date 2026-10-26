package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.stereotype.Service;

/**
 * METHOD OVERLOADING anchor: three assessRisk(...) overloads, same name, different signatures.
 *  - assessRisk(Long id)
 *  - assessRisk(Long id, String tier)   (deprecated alias of assessRiskForTier)
 *  - assessRisk(LoanDto app)
 */
@Service
public class RiskService {

    static final double DEFAULT_ASSESSMENT_AMOUNT = 50_000.0;

    private final RiskAssessor assessor;

    public RiskService(RiskAssessor assessor) {
        this.assessor = assessor;
    }

    /** Overload 1: assess by loan id only (defaults). */
    public RiskAssessmentDto assessRisk(Long id) {
        return assessRiskForTier(id, "STANDARD");
    }

    /** Assess by loan id + pricing tier, using the default exposure for that tier. */
    public RiskAssessmentDto assessRiskForTier(Long id, String tier) {
        String effectiveTier = tier == null || tier.isBlank() ? "STANDARD" : tier;
        LoanDto loan = new LoanDto(id, DEFAULT_ASSESSMENT_AMOUNT, effectiveTier, null);
        return assessRisk(loan);
    }

    /**
     * Overload 2: kept for existing callers; same behaviour as {@link #assessRiskForTier}.
     *
     * @deprecated use {@link #assessRiskForTier(Long, String)}; the tier-specific name makes
     * the overload set unambiguous at call sites. Scheduled for removal next quarter.
     */
    @Deprecated
    public RiskAssessmentDto assessRisk(Long id, String tier) {
        return assessRiskForTier(id, tier);
    }

    /** Overload 3: assess a full loan application. */
    public RiskAssessmentDto assessRisk(LoanDto app) {
        // A decision without a loan id can't be matched back to any loan by callers.
        if (app.getId() == null) {
            throw new IllegalArgumentException("loan id is required for risk assessment");
        }
        return assessor.assess(app);
    }
}
