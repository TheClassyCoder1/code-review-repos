package com.example.lending.loan.part7;

import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Portfolio breakdown used by the collections desk to size its queues: loan counts and
 * exposure per tier and status. Read-only; returns aggregates only.
 */
@RestController
@RequestMapping("/api/v1/loans/portfolio")
public class LoanPortfolioController {

    private final EntityManager entityManager;

    public LoanPortfolioController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/summary")
    public List<Map<String, Object>> summary(@RequestParam(required = false) String status) {
        String jpql = "SELECT l.tier, l.status, COUNT(l), SUM(l.amount) FROM Loan l"
                + (status == null ? "" : " WHERE l.status = :status")
                + " GROUP BY l.tier, l.status ORDER BY l.tier, l.status";
        var query = entityManager.createQuery(jpql, Object[].class);
        if (status != null) {
            query.setParameter("status", status);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : query.getResultList()) {
            rows.add(Map.of(
                    "tier", row[0] == null ? "UNKNOWN" : row[0],
                    "status", row[1] == null ? "UNKNOWN" : row[1],
                    "loanCount", row[2],
                    "exposure", row[3] == null ? 0.0 : row[3]));
        }
        return rows;
    }
}
