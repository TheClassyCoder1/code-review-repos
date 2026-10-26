package com.example.lending.loan.part7.repayment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentPlanRepository extends JpaRepository<RepaymentPlan, Long> {

    Optional<RepaymentPlan> findFirstByLoanIdAndStatus(Long loanId, String status);

    List<RepaymentPlan> findByLoanIdOrderByCreatedAtDesc(Long loanId);
}
