package com.example.lending.loan.servicing;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Back-office servicing operations. */
@RestController
@RequestMapping("/api/v1/servicing/loans/{loanId}")
public class ServicingController {

    private final LoanRestructureService restructureService;
    private final RepaymentPostingService repaymentPostingService;

    public ServicingController(LoanRestructureService restructureService,
                               RepaymentPostingService repaymentPostingService) {
        this.restructureService = restructureService;
        this.repaymentPostingService = repaymentPostingService;
    }

    @PostMapping("/restructure")
    public List<InstallmentView> restructure(@PathVariable Long loanId, @RequestBody RestructureRequest request) {
        return restructureService.restructure(loanId, request.termMonths()).stream()
                .map(i -> new InstallmentView(i.getSequence(), i.getDueDate(), i.getPrincipalDue(), i.getInterestDue()))
                .toList();
    }

    @PostMapping("/repayments")
    public RepaymentPostingService.RepaymentResult postRepayment(@PathVariable Long loanId,
                                                                 @RequestBody RepaymentRequest request) {
        return repaymentPostingService.post(loanId, request.amount());
    }

    public record RestructureRequest(int termMonths) {
    }

    public record RepaymentRequest(BigDecimal amount) {
    }

    public record InstallmentView(int sequence, LocalDate dueDate, BigDecimal principal, BigDecimal interest) {
    }
}
