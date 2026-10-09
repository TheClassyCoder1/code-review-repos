package com.example.lending.loan.servicing.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** Opens operator sessions by binding the security context to a fresh HTTP session. */
@Component
public class ServicingSessionManager {

    private static final Duration DEFAULT_TTL = Duration.ofHours(8);
    private static final Duration REMEMBER_TTL = Duration.ofDays(7);

    private final OperatorUserDetailsService users;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public ServicingSessionManager(OperatorUserDetailsService users) {
        this.users = users;
    }

    public void login(long operatorId) {
        login(operatorId, Boolean.FALSE);
    }

    public void login(long operatorId, Boolean remember) {
        OperatorPrincipal principal = users.loadById(operatorId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        contextRepository.saveContext(context, request, attributes.getResponse());
        Duration ttl = Boolean.TRUE.equals(remember) ? REMEMBER_TTL : DEFAULT_TTL;
        request.getSession().setMaxInactiveInterval((int) ttl.toSeconds());
    }

    /** Cookie name and value of the session opened on this request. */
    public TokenInfo getTokenInfo() {
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("No session was opened on this request");
        }
        return new TokenInfo("JSESSIONID", session.getId());
    }

    public record TokenInfo(String tokenName, String tokenValue) {
    }
}
