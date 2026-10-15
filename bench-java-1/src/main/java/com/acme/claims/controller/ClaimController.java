package com.acme.claims.controller;

import com.acme.claims.client.EvidenceFetchClient;
import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.DraftClaim;
import com.acme.claims.model.NewClaim;
import com.acme.claims.model.Payout;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.security.ClaimAccess;
import com.acme.claims.security.RequestUser;
import com.acme.claims.service.ClaimQueryService;
import com.acme.claims.service.ClaimSnapshotCodec;
import com.acme.claims.service.DocumentStorageService;
import com.acme.claims.service.PayoutService;
import com.acme.claims.service.RepairEstimateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimRepository claims;
    private final ClaimAccess access;
    private final ClaimQueryService queries;
    private final DocumentStorageService storage;
    private final PayoutService payoutService;
    private final EvidenceFetchClient evidence;
    private final RepairEstimateService repairEstimates;
    private final ClaimSnapshotCodec codec;

    public ClaimController(ClaimRepository claims, ClaimAccess access, ClaimQueryService queries,
                           DocumentStorageService storage, PayoutService payoutService,
                           EvidenceFetchClient evidence, RepairEstimateService repairEstimates,
                           ClaimSnapshotCodec codec) {
        this.claims = claims;
        this.access = access;
        this.queries = queries;
        this.storage = storage;
        this.payoutService = payoutService;
        this.evidence = evidence;
        this.repairEstimates = repairEstimates;
        this.codec = codec;
    }

    public record EvidenceRequest(String url) {
    }

    public record RepairEstimateRequest(String shopHost, long estimateId) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> submit(@Valid @RequestBody NewClaim claim, HttpServletRequest request) {
        RequestUser user = RequestUser.from(request);
        if (user.isAdjuster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "adjusters cannot file claims");
        }
        long id = claims.insert(claim, user.id());
        return ResponseEntity.created(URI.create("/api/claims/" + id)).body(Map.of("id", id));
    }

    @GetMapping("/{id}")
    public Claim get(@PathVariable long id, HttpServletRequest request) {
        return access.requireOwned(id, RequestUser.from(request));
    }

    @GetMapping("/search")
    public List<Claim> search(@RequestParam(required = false) String lastName,
                              @RequestParam(defaultValue = "SUBMITTED") ClaimStatus status,
                              @RequestParam(defaultValue = "created") String sort,
                              @RequestParam(defaultValue = "true") boolean descending,
                              HttpServletRequest request) {
        if (!RequestUser.from(request).isAdjuster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return queries.search(lastName, status, sort, descending);
    }

    @GetMapping("/{id}/documents")
    public List<String> listDocuments(@PathVariable long id) throws IOException {
        return storage.listForClaim(id);
    }

    @GetMapping("/{id}/payouts")
    public List<Payout> payouts(@PathVariable long id, HttpServletRequest request) {
        access.requireOwned(id, RequestUser.from(request));
        return payoutService.listForClaim(id);
    }

    @PostMapping("/{id}/evidence")
    public Map<String, String> attachEvidence(@PathVariable long id, @RequestBody EvidenceRequest body,
                                              HttpServletRequest request) throws IOException {
        access.requireOwned(id, RequestUser.from(request));
        byte[] photo = evidence.fetchPhoto(body.url());
        String documentId = UUID.randomUUID().toString();
        storage.store(id, documentId + "." + EvidenceFetchClient.contentTypeFor(body.url()), photo);
        return Map.of("document_id", documentId);
    }

    @PostMapping("/{id}/repair-estimate")
    public String repairEstimate(@PathVariable long id, @RequestBody RepairEstimateRequest body,
                                 HttpServletRequest request) {
        access.requireOwned(id, RequestUser.from(request));
        return repairEstimates.fetchEstimate(body.shopHost(), body.estimateId());
    }

    @PutMapping("/draft")
    public ResponseEntity<Void> saveDraft(@RequestBody DraftClaim draft, HttpServletRequest request) {
        if (codec.isEmptyDraft(draft)) {
            return ResponseEntity.badRequest().build();
        }
        claims.saveDraft(RequestUser.from(request).id(), codec.encodeDraft(draft));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/draft")
    public ResponseEntity<DraftClaim> loadDraft(HttpServletRequest request) {
        return claims.loadDraft(RequestUser.from(request).id())
                .map(codec::restoreDraft)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
