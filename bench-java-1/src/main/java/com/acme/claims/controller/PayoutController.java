package com.acme.claims.controller;

import com.acme.claims.model.Payout;
import com.acme.claims.model.PayoutInstruction;
import com.acme.claims.repository.PayoutRepository;
import com.acme.claims.service.PayoutService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/payouts")
public class PayoutController {

    private final PayoutRepository payouts;
    private final PayoutService payoutService;

    public PayoutController(PayoutRepository payouts, PayoutService payoutService) {
        this.payouts = payouts;
        this.payoutService = payoutService;
    }

    @GetMapping
    public List<Payout> recent(@RequestParam(defaultValue = "SENT") String status,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return payouts.recent(status, limit);
    }

    @PostMapping("/{payoutId}/disburse")
    public ResponseEntity<Void> disburse(@PathVariable long payoutId, @RequestBody PayoutInstruction instruction) {
        payoutService.disburse(payoutId, instruction);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{payoutId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable long payoutId) {
        payoutService.cancel(payoutId);
        return ResponseEntity.noContent().build();
    }
}
