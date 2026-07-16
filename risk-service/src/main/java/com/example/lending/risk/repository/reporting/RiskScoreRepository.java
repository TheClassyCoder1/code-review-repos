package com.example.lending.risk.repository.reporting;

import com.example.lending.risk.reporting.entity.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bound to the REPORTING datasource (db-secondary, schema lending). */
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
}
