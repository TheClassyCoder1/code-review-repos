package com.example.lending.loan.banklink;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BankLinkService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    private final BankLinkSessionRepository sessionRepository;
    private final Clock clock;
    private final String authorizeUrl;
    private final String clientId;
    private final String redirectUri;
    private final Set<String> allowedReturnHosts;

    public BankLinkService(BankLinkSessionRepository sessionRepository,
                           Clock clock,
                           @Value("${lending.bank-link.authorize-url}") String authorizeUrl,
                           @Value("${lending.bank-link.client-id}") String clientId,
                           @Value("${lending.bank-link.redirect-uri}") String redirectUri,
                           @Value("${lending.portal.allowed-return-hosts}") Set<String> allowedReturnHosts) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
        this.authorizeUrl = authorizeUrl;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.allowedReturnHosts = allowedReturnHosts;
    }

    @Transactional
    public BankLinkStart start(Long borrowerId, String returnUrl) {
        URI target = portalReturnUrl(returnUrl);

        BankLinkSession session = new BankLinkSession();
        session.setState(UUID.randomUUID().toString());
        session.setBorrowerId(borrowerId);
        session.setReturnUrl(target.toString());
        session.setStatus("PENDING");
        session.setExpiresAt(clock.instant().plus(SESSION_TTL));
        sessionRepository.save(session);

        String url = UriComponentsBuilder.fromHttpUrl(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", session.getState())
                .encode()
                .toUriString();
        return new BankLinkStart(url);
    }

    @Transactional
    public BankLinkSession complete(String state, String code) {
        BankLinkSession session = sessionRepository.findById(state)
                .filter(s -> "PENDING".equals(s.getStatus()))
                .filter(s -> s.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank link session is invalid or expired"));
        session.setAuthorizationCode(code);
        session.setStatus("AUTHORIZED");
        return session;
    }

    private URI portalReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "returnUrl is required");
        }
        URI uri;
        try {
            uri = new URI(returnUrl);
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "returnUrl is not a valid URL");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null || !allowedReturnHosts.contains(host)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "returnUrl must point to the borrower portal");
        }
        return uri;
    }

    public record BankLinkStart(String authorizationUrl) {
    }
}
