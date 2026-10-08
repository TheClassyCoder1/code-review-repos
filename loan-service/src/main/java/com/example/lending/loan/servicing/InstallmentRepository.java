package com.example.lending.loan.servicing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByLoanIdAndStatusOrderByDueDateAsc(Long loanId, String status);

    List<Installment> findByLoanIdAndStatusInOrderByDueDateAsc(Long loanId, Collection<String> statuses);
}
