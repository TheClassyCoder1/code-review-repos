package com.example.lending.loan.part7.repository;

import com.example.lending.loan.part7.entity.RepaymentInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, Long> {

    List<RepaymentInstallment> findByLoanIdOrderBySequenceNoAsc(Long loanId);

    List<RepaymentInstallment> findByLoanIdAndStatusOrderBySequenceNoAsc(Long loanId, String status);

    List<RepaymentInstallment> findByStatusAndDueDateBefore(String status, LocalDate date);

    boolean existsByLoanId(Long loanId);

    @Modifying
    @Query("update RepaymentInstallment i set i.status = :status "
            + "where i.loanId = :loanId and i.status = 'DUE'")
    int markOpenInstallments(@Param("loanId") Long loanId, @Param("status") String status);
}
