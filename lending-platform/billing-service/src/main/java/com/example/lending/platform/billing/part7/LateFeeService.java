package com.example.lending.platform.billing.part7;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LateFeeService {

    private final LateFeePolicy policy;

    public LateFeeService(LateFeePolicy policy) {
        this.policy = policy;
    }

    /**
     * Assesses each missed installment once. Duplicate (loanId, sequenceNo) pairs in the batch are ignored so
     * a retried collections export cannot double-charge.
     */
    public List<LateFeeAssessment> assess(List<MissedInstallment> missed, LocalDate asOf) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        Set<String> seen = new HashSet<>();
        List<LateFeeAssessment> result = new ArrayList<>(missed.size());
        for (MissedInstallment m : missed) {
            if (m.getLoanId() == null || m.getDueDate() == null || m.getAmountDue() == null) {
                throw new IllegalArgumentException("missed installment requires loanId, dueDate and amountDue");
            }
            if (!seen.add(m.getLoanId() + "#" + m.getSequenceNo())) {
                continue;
            }
            long daysPastDue = Math.max(0, ChronoUnit.DAYS.between(m.getDueDate(), today));
            BigDecimal fee = policy.feeFor(m.getAmountDue(), daysPastDue);
            result.add(new LateFeeAssessment(m.getLoanId(), m.getSequenceNo(), m.getDueDate(), daysPastDue,
                    m.getAmountDue(), fee, daysPastDue <= policy.getGraceDays()));
        }
        return result;
    }

    public BigDecimal total(List<LateFeeAssessment> assessments) {
        return assessments.stream()
                .map(LateFeeAssessment::fee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
