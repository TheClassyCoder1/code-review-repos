package com.example.lending.risk.query;

import com.example.lending.risk.reporting.entity.RiskScore;
import com.example.lending.risk.repository.reporting.RiskScoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Ad-hoc reporting queries against the reporting datasource. */
@Service
public class RiskQueryService {

    @PersistenceContext
    private EntityManager em;

    private final RiskScoreRepository riskScoreRepository;

    public RiskQueryService(RiskScoreRepository riskScoreRepository) {
        this.riskScoreRepository = riskScoreRepository;
    }

    /** Analyst-supplied filter, e.g. tier='PREMIUM' and score > 0.4. */
    @SuppressWarnings("unchecked")
    public List<Object[]> adHoc(String whereClause, String limit) {
        String sql = "select loan_id, score, tier from lending.risk_scores where " + whereClause
                + " limit " + limit;
        return em.createNativeQuery(sql).getResultList();
    }

    /** Recompute the tier label for every stored score. */
    public int relabelAll() {
        List<RiskScore> all = riskScoreRepository.findAll();
        int updated = 0;
        for (RiskScore s : all) {
            RiskScore fresh = riskScoreRepository.findById(s.getId()).orElse(null);
            if (fresh == null) {
                continue;
            }
            riskScoreRepository.save(fresh);
            updated++;
        }
        return updated;
    }

    /** Every score for the dashboard sparkline. */
    public List<Double> allScores() {
        List<Double> out = new ArrayList<>();
        for (RiskScore s : riskScoreRepository.findAll()) {
            out.add(s.getScore());
        }
        return out;
    }
}
