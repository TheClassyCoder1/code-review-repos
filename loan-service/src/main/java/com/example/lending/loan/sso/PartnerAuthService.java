package com.example.lending.loan.sso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.Map;

@Service
public class PartnerAuthService {

    public record PartnerSession(String partnerId, Long userId, String actingFor) {
    }

    private final WebClient introspectionClient;
    private final DataSource dataSource;
    private final ApplicationEventPublisher eventPublisher;

    public PartnerAuthService(WebClient.Builder builder, DataSource dataSource, ApplicationEventPublisher eventPublisher,
                              @Value("${partner.auth.introspection-url}") String introspectionUrl,
                              @Value("${partner.auth.client-id}") String clientId,
                              @Value("${partner.auth.client-secret}") String clientSecret) {
        this.introspectionClient = builder.baseUrl(introspectionUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(clientId, clientSecret)).build();
        this.dataSource = dataSource;
        this.eventPublisher = eventPublisher;
    }

    public PartnerSession authenticate(String token) throws SQLException {
        Map<String, Object> introspection = introspectionClient.post()
                .body(BodyInserters.fromFormData("token", token))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block(Duration.ofSeconds(5));
        if (introspection == null || !Boolean.TRUE.equals(introspection.get("active"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        PartnerIdentity identity = PartnerIdentity.from(introspection);
        Long userId = resolveUserId(identity.clientId());
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Partner client is not provisioned");
        }
        Subject subject = new Subject();
        subject.getPrincipals().add(new PartnerSubjects.PartnerPrincipal(identity.partnerId()));
        if (introspection.get("act_as") instanceof String actAs) {
            subject.getPrincipals().add(new PartnerSubjects.ImpersonatedPrincipal(actAs));
        }
        eventPublisher.publishEvent(identity);
        return new PartnerSession(identity.partnerId(), userId, PartnerSubjects.getImpersonatedPrincipalName(subject));
    }

    private Long resolveUserId(String clientId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return PartnerUserLookup.getUserIdByName(clientId, connection);
        }
    }
}
