package com.example.lending.loan.borrower;

import com.example.lending.loan.security.TokenHashes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

/**
 * Resolves the borrower behind a bearer session token and exposes the id as the
 * {@value #BORROWER_ID} request attribute. Requests without a live session are rejected.
 */
public class SessionTokenFilter extends OncePerRequestFilter {

    public static final String BORROWER_ID = "borrowerId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final BorrowerSessionRepository sessionRepository;
    private final Clock clock;

    public SessionTokenFilter(BorrowerSessionRepository sessionRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Optional<BorrowerSession> session = sessionRepository.findById(TokenHashes.sha256Hex(token))
                .filter(s -> s.getExpiresAt().isAfter(clock.instant()));
        if (session.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setAttribute(BORROWER_ID, session.get().getBorrowerId());
        chain.doFilter(request, response);
    }
}
