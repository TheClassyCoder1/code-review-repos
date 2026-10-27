package com.example.lending.ops.partner.jwt;

import com.example.lending.ops.partner.jwt.token.JWT;
import com.example.lending.ops.partner.jwt.token.JWT.*;
import jakarta.servlet.http.HttpSession;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/** Issues the short-lived login JWT handed to partner apps during SSO. */
@Service
@RequiredArgsConstructor
public class JwtLoginService {
    private static final Logger _logger = LoggerFactory.getLogger(JwtLoginService.class);

    private final JwtSignerValidationService jwtSignerValidationService;
    @Getter
    @Value("${ops.jwt.issuer}")
    private final String issuer;

    public String buildLoginJwt() {
        _logger.debug("build Login JWT .");

        DateTime currentDateTime = DateTime.now();
        Date expirationTime = currentDateTime.plusMinutes(5).toDate();
        _logger.debug("Expiration Time : " + expirationTime);
        JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder().subject(WebContext.getSession().getId())
                .expirationTime(expirationTime).issuer(getIssuer())
                .issueTime(currentDateTime.toDate()).jwtID(UUID.randomUUID().toString()).build();

        JWT jwtToken = new PlainJWT(jwtClaims);

        _logger.info("JWT Claims : " + jwtClaims.toString());

        JWSAlgorithm signingAlg = jwtSignerValidationService.getDefaultSigningAlgorithm();

        jwtToken = new SignedJWT(new JWSHeader(signingAlg), jwtClaims);
        // sign it with the server's key
        jwtSignerValidationService.signJwt((SignedJWT) jwtToken);

        String tokenString = jwtToken.serialize();
        _logger.debug("JWT Token : " + tokenString);
        return tokenString;
    }

    @Service
    @RequiredArgsConstructor
    public static class JwtSignerValidationService {
        @Value("${ops.jwt.signing-key}")
        private final String signingKey;

        public JWSAlgorithm getDefaultSigningAlgorithm() { return JWSAlgorithm.HS256; }

        public void signJwt(SignedJWT jwt) {
            try {
                String alg = jwt.getHeader().algorithm().jcaName();
                Mac mac = Mac.getInstance(alg);
                mac.init(new SecretKeySpec(signingKey.getBytes(), alg));
                jwt.setSignature(Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(jwt.getSigningInput().getBytes())));
            } catch (Exception e) {
                throw new IllegalStateException("unable to sign login token", e);
            }
        }
    }

    public static final class WebContext {
        public static String version() { return "ops-console 1.0.0"; }

        public static HttpSession getSession() {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(true);
        }
    }
}
