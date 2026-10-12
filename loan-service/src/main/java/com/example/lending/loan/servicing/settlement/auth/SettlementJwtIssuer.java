package com.example.lending.loan.servicing.settlement.auth;

import com.example.lending.loan.servicing.settlement.auth.SettlementJwtSigner.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Issues the short-lived token that signs an operator in to the partner settlement portal. */
@Service
public class SettlementJwtIssuer {

    private static final Logger _logger = LoggerFactory.getLogger(SettlementJwtIssuer.class);

    private final SettlementJwtSigner jwtSignerValidationService;
    private final String issuer;

    public SettlementJwtIssuer(SettlementJwtSigner jwtSignerValidationService,
                               @Value("${servicing.settlement.jwt.issuer}") String issuer) {
        this.jwtSignerValidationService = jwtSignerValidationService;
        this.issuer = issuer;
    }

    public String buildLoginJwt() {
        _logger.debug("build Login JWT .");

        Instant currentDateTime = Instant.now();
        Instant expirationTime = currentDateTime.plus(5, ChronoUnit.MINUTES);
        _logger.debug("Expiration Time : " + expirationTime);
        JwtClaims jwtClaims = new JwtClaims.Builder().subject(RequestContextHolder.currentRequestAttributes().getSessionId())
                .expirationTime(expirationTime).issuer(getIssuer())
                .issueTime(currentDateTime).jwtID(UUID.randomUUID().toString()).build();

        _logger.info("JWT Claims : " + jwtClaims.toString());

        String signingAlg = jwtSignerValidationService.getDefaultSigningAlgorithm();

        String tokenString = jwtSignerValidationService.sign(signingAlg, jwtClaims);
        _logger.debug("JWT Token : " + tokenString);
        return tokenString;
    }

    private String getIssuer() {
        return issuer;
    }
}
