package com.example.lending.loan.servicing.statements.portal;

import com.example.lending.loan.servicing.common.Digests;
import com.example.lending.loan.servicing.security.portal.PortalTokens;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/** Verifies portal credentials and issues an access token. */
@Service
public class PortalLoginService {

    private final PortalUserRepository repository;
    private final PortalTokens portalTokens;

    public PortalLoginService(PortalUserRepository repository, PortalTokens portalTokens) {
        this.repository = repository;
        this.portalTokens = portalTokens;
    }

    public Optional<String> login(String username, String password) {
        Optional<PortalUser> user = repository.findByUsername(username);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        byte[] expected = user.get().getPassword().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = Digests.sha256Hex(password).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            return Optional.empty();
        }
        return Optional.of(portalTokens.issue(username));
    }
}
