package com.example.lending.risk.repository.reporting.part7;

import com.example.lending.risk.reporting.entity.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-side queries over risk_scores on the REPORTING datasource (picked up by
 * ReportingDataSourceConfig's basePackages scan, which includes sub-packages).
 */
public interface RiskScoreHistoryRepository extends JpaRepository<RiskScore, Long> {

    /** Newest first; the id is monotonic so it doubles as an insertion-order key. */
    List<RiskScore> findTop20ByLoanIdOrderByIdDesc(Long loanId);

    long countByLoanId(Long loanId);

    @Query("select distinct s.loanId from RiskScore s where s.decision = :decision")
    List<Long> findLoanIdsWithDecision(@Param("decision") String decision);
}
