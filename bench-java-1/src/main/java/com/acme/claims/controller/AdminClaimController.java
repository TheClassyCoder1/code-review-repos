package com.acme.claims.controller;

import com.acme.claims.model.Claim;
import com.acme.claims.model.LegacyClaim;
import com.acme.claims.model.PayoutInstruction;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.service.ClaimSnapshotCodec;
import com.acme.claims.service.PayoutService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/claims")
public class AdminClaimController {

    private final ClaimRepository claims;
    private final ClaimSnapshotCodec codec;
    private final PayoutService payoutService;

    public AdminClaimController(ClaimRepository claims, ClaimSnapshotCodec codec, PayoutService payoutService) {
        this.claims = claims;
        this.codec = codec;
        this.payoutService = payoutService;
    }

    public record PayoutRequest(BigDecimal amount, String payeeName, String accountNumber, String routingNumber) {
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Void> reassign(@PathVariable long id, @RequestParam long adjusterId) {
        claims.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        claims.assignAdjuster(id, adjusterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public Map<String, Integer> importLegacy(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            List<LegacyClaim> batch = codec.importLegacyBatch(in);
            return Map.of("received", batch.size(), "valid", codec.countValid(batch));
        }
    }

    @PostMapping("/{id}/payouts")
    public Map<String, Long> approvePayout(@PathVariable long id, @RequestBody PayoutRequest body) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long payoutId = payoutService.approve(claim, body.amount());
        payoutService.queue(payoutId, new PayoutInstruction(
                id, body.payeeName(), body.accountNumber(), body.routingNumber(), body.amount()));
        return Map.of("payout_id", payoutId);
    }
}
