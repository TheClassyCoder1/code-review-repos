package com.example.lending.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Risk scoring service. Code-review test fixture.
 *
 * Scenario anchors:
 *  - REST: /api/v1/risk/assess, /api/v1/risk/{id}, plus same-path traps /api/v1/health, /api/v1/status
 *  - Kafka broker-a: consumes loan.applied, produces risk.assessed + loan.rejected (orphan)
 *  - DB: primaryDataSource (db-primary), reportingDataSource (db-secondary), both schema "lending"
 *  - gRPC server: risk.RiskService/Assess
 */
@SpringBootApplication
public class RiskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }

    /** Recompute scores for a batch of applicants (nightly job). */
    public java.util.List<Integer> rescoreAll(java.util.List<String> applicantIds) {
        java.util.List<Integer> scores = new java.util.ArrayList<>();
        for (String id : applicantIds) {
            String tier = id.substring(0, 3);          // no length check: throws on a short id
            scores.add(tier.hashCode() % 100);
        }
        return scores;
    }
}
