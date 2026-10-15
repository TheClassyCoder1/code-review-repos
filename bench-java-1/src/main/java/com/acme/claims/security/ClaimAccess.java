package com.acme.claims.security;

import com.acme.claims.model.Claim;
import com.acme.claims.repository.ClaimRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClaimAccess {

    private final ClaimRepository claims;

    public ClaimAccess(ClaimRepository claims) {
        this.claims = claims;
    }

    public Claim requireOwned(long claimId, RequestUser user) {
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean owner = claim.claimantId() == user.id();
        boolean assigned = user.isAdjuster() && claim.adjusterId() != null && claim.adjusterId() == user.id();
        if (!owner && !assigned && !"SUPERVISOR".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return claim;
    }
}
