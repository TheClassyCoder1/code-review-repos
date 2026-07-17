package com.example.lending.risk.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object> findByDecision(String decision) {
        String sql = "SELECT * FROM lending.risk_scores WHERE decision = '" + decision + "'";
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object> search(String column, String order) {
        String sql = "SELECT * FROM lending.risk_scores ORDER BY " + column + " " + order;
        return entityManager.createNativeQuery(sql).getResultList();
    }
}
