package com.example.lending.loan.part6.api;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.part6.analytics.AnonymousFunnelEvent;
import com.example.lending.loan.part6.analytics.LoanFunnelAnalytics;
import com.example.lending.loan.part6.kyc.BorrowerIdentity;
import com.example.lending.loan.part6.kyc.BorrowerIdentityRepository;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class LoanV2Service {

    private final LoanRepository loanRepository;
    private final BorrowerIdentityRepository borrowerRepository;
    private final LoanFunnelAnalytics funnelAnalytics;
    private final String currency;

    public LoanV2Service(LoanRepository loanRepository,
                         BorrowerIdentityRepository borrowerRepository,
                         LoanFunnelAnalytics funnelAnalytics,
                         @Value("${loan.currency:GBP}") String currency) {
        this.loanRepository = loanRepository;
        this.borrowerRepository = borrowerRepository;
        this.funnelAnalytics = funnelAnalytics;
        this.currency = currency;
    }

    public Optional<LoanViewV2> find(Long id) {
        Optional<Loan> loan = loanRepository.findById(id);
        loan.filter(l -> l.getUserId() != null)
                .ifPresent(l -> funnelAnalytics.track(AnonymousFunnelEvent.forLoan(
                        AnonymousFunnelEvent.Step.LOAN_VIEWED_V2, l.getUserId(), l.getId())));
        return loan.map(this::toView);
    }

    private LoanViewV2 toView(Loan loan) {
        String verification = borrowerRepository.findByUserId(loan.getUserId())
                .map(BorrowerIdentity::getVerificationStatus)
                .orElse("UNREGISTERED");
        BigDecimal amount = BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_UP);
        return new LoanViewV2(loan.getId(), loan.getUserId(), new LoanViewV2.Money(amount, currency),
                loan.getTier(), loan.getStatus(), verification);
    }
}
