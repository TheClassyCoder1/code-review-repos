package com.example.lending.loan.servicing.security.portal;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.statements.portal.PortalUser;
import com.example.lending.loan.servicing.statements.portal.PortalUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Authenticates borrower portal requests from the {@link PortalTokens#HEADER} header. */
@Component
public class PortalTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String[] BORROWER_AUTHORITIES = {"statement:read", "hardship:query", "hardship:create"};

    private final PortalTokens portalTokens;
    private final PortalUserRepository portalUsers;

    public PortalTokenAuthenticationFilter(PortalTokens portalTokens, PortalUserRepository portalUsers) {
        this.portalTokens = portalTokens;
        this.portalUsers = portalUsers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getHeader(PortalTokens.HEADER) != null) {
            try {
                String username = portalTokens.getUserNameByToken(request);
                Optional<PortalUser> user = portalUsers.findByUsername(username);
                if (user.isPresent()) {
                    PortalUser portalUser = user.get();
                    PortalPrincipal principal = new PortalPrincipal(
                            portalUser.getId(), portalUser.getBorrowerId(), portalUser.getUsername());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, AuthorityUtils.createAuthorityList(BORROWER_AUTHORITIES)));
                    SecurityContextHolder.setContext(context);
                }
            } catch (ServicingException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
