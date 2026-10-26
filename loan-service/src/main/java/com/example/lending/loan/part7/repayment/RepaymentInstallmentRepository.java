package com.example.lending.loan.part7.repayment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, Long> {

    List<RepaymentInstallment> findByPlanIdOrderBySequenceNoAsc(Long planId);

    List<RepaymentInstallment> findByPlanIdAndStatusOrderBySequenceNoAsc(Long planId, String status);

    List<RepaymentInstallment> findByStatusAndDueDateBefore(String status, LocalDate cutoff);

    long countByPlanIdAndStatus(Long planId, String status);
}
