package com.acme.claims.service;

import com.acme.claims.client.PaymentGatewayClient;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.Payout;
import com.acme.claims.model.PayoutInstruction;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.repository.PayoutRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutRepository payouts;
    private final ClaimRepository claims;
    private final PaymentGatewayClient gateway;

    public PayoutService(PayoutRepository payouts, ClaimRepository claims, PaymentGatewayClient gateway) {
        this.payouts = payouts;
        this.claims = claims;
        this.gateway = gateway;
    }

    @Transactional
    public long approve(Claim claim, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payout must be positive");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal alreadyApproved = payouts.sumApproved(claim.id());
        if (alreadyApproved.add(rounded).compareTo(claim.amountClaimed()) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "payout exceeds claimed amount");
        }
        long payoutId = payouts.insert(claim.id(), rounded, "APPROVED");
        claims.updateStatus(claim.id(), ClaimStatus.APPROVED);
        return payoutId;
    }

    public BigDecimal remainingFor(Claim claim) {
        BigDecimal remaining = claim.amountClaimed().subtract(payouts.sumApproved(claim.id()));
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    public List<Payout> listForClaim(long claimId) {
        return payouts.findByClaim(claimId).stream()
                .filter(p -> !"CANCELLED".equals(p.status()))
                .toList();
    }

    public void cancel(long payoutId) {
        payouts.markStatus(payoutId, "CANCELLED", null);
        log.info("Payout {} cancelled", payoutId);
    }

    public void queue(long payoutId, PayoutInstruction instruction) {
        payouts.markStatus(payoutId, "QUEUED", null);
        log.info("Queued payout {}: {}", payoutId, instruction);
    }

    public boolean isDisbursable(Payout payout) {
        return "QUEUED".equals(payout.status()) || "APPROVED".equals(payout.status());
    }

    public void disburse(long payoutId, PayoutInstruction instruction) {
        PaymentGatewayClient.TransferResult result = gateway.submitTransfer(payoutId, instruction);
        log.info("Payout {} sent: account={} routing={} authorization={}", payoutId,
                instruction.accountNumber(), instruction.routingNumber(), result.authorizationCode());
        payouts.markStatus(payoutId, "SENT", result.reference());
    }
}
