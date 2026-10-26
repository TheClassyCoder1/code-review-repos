package com.example.lending.platform.billing.part7;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Result of assessing late fees for one missed installment. */
public record LateFeeAssessment(Long loanId,
                                int sequenceNo,
                                LocalDate dueDate,
                                long daysPastDue,
                                BigDecimal amountMissed,
                                BigDecimal fee,
                                boolean withinGrace) {
}
