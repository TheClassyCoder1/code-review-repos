package com.example.lending.loan.servicing;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** Applies a repayment to the oldest installments first: interest, then principal. */
@Service
public class RepaymentAllocationService {

    private final InstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;

    public RepaymentAllocationService(InstallmentRepository installmentRepository, LoanRepository loanRepository) {
        this.installmentRepository = installmentRepository;
        this.loanRepository = loanRepository;
    }

    /** Returns the part of {@code amount} that did not fit any installment. */
    public BigDecimal allocate(Loan loan, BigDecimal amount) {
        List<Installment> due = installmentRepository.findByLoanIdAndStatusInOrderByDueDateAsc(
                loan.getId(), List.of(Installment.OPEN, Installment.PARTIAL));

        BigDecimal remaining = amount;
        boolean allSettled = true;
        for (Installment installment : due) {
            if (remaining.signum() > 0) {
                BigDecimal toInterest = remaining.min(installment.interestOutstanding());
                installment.setInterestPaid(installment.getInterestPaid().add(toInterest));
                remaining = remaining.subtract(toInterest);

                BigDecimal toPrincipal = remaining.min(installment.principalOutstanding());
                installment.setPrincipalPaid(installment.getPrincipalPaid().add(toPrincipal));
                remaining = remaining.subtract(toPrincipal);

                boolean settled = installment.interestOutstanding().signum() == 0
                        && installment.principalOutstanding().signum() == 0;
                installment.setStatus(settled ? Installment.PAID : Installment.PARTIAL);
                installmentRepository.save(installment);
            }
            allSettled &= Installment.PAID.equals(installment.getStatus());
        }

        if (allSettled) {
            loan.setStatus("PAID_OFF");
            loanRepository.save(loan);
        }
        return remaining;
    }
}
