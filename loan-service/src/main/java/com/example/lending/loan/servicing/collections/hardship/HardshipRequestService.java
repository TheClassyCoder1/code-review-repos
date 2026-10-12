package com.example.lending.loan.servicing.collections.hardship;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Hardship requests submitted from the borrower portal. */
@Service
public class HardshipRequestService {

    public record HardshipRequestCreate(@NotNull Long loanId,
                                        @NotBlank @Size(max = 2000) String reason,
                                        @PositiveOrZero BigDecimal monthlyIncome,
                                        @Min(1) @Max(12) Integer requestedMonths) {
    }

    private final HardshipRequestRepository repository;

    public HardshipRequestService(HardshipRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public HardshipRequest getHardshipRequest(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<HardshipRequest> getHardshipRequestsOfBorrower(Long borrowerId) {
        return repository.findByBorrowerIdOrderByCreatedAtDesc(borrowerId);
    }

    @Transactional
    public Long createHardshipRequest(Long borrowerId, HardshipRequestCreate create) {
        HardshipRequest request = new HardshipRequest();
        request.setBorrowerId(borrowerId);
        request.setLoanId(create.loanId());
        request.setReason(create.reason());
        request.setMonthlyIncome(create.monthlyIncome());
        request.setRequestedMonths(create.requestedMonths());
        return repository.save(request).getId();
    }
}
