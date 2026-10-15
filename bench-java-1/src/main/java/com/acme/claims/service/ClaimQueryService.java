package com.acme.claims.service;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.repository.ClaimRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimQueryService {

    private static final Map<String, String> SORTABLE = Map.of(
            "created", "created_at",
            "amount", "amount_claimed",
            "policy", "policy_number",
            "status", "status");

    private final ClaimRepository claims;

    public ClaimQueryService(ClaimRepository claims) {
        this.claims = claims;
    }

    public List<Claim> search(String lastName, ClaimStatus status, String sort, boolean descending) {
        if (lastName != null && !lastName.isBlank()) {
            return claims.searchByClaimant(lastName.trim(), status);
        }
        String column = SORTABLE.get(sort);
        if (column == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported sort: " + sort);
        }
        return claims.findByStatusOrdered(status, column, descending);
    }
}
