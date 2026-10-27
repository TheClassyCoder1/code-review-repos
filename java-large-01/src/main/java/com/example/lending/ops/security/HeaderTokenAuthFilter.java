package com.example.lending.ops.security;

import com.example.lending.ops.scheduler.XxlSsoHelper.Sha256Tool;
import com.example.lending.ops.security.LoginUserUtils.ApiCaller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

/**
 * Authenticates every request outside the console's session chain. Credentials are read only from
 * the {@code Authorization: Bearer} header: cookies are never consulted and no HTTP session is
 * created, so a cross-site request carries no ambient credentials.
 */
@Component
@RequiredArgsConstructor
public class HeaderTokenAuthFilter extends OncePerRequestFilter {
    private static final List<String> OPEN_PREFIXES = List.of("/console/", "/auth/", "/sys/sms", "/scheduler/", "/metadata/");

    private final JdbcTemplate jdbcTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return OPEN_PREFIXES.stream().anyMatch(request.getRequestURI()::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        List<ApiCaller> callers = header == null || !header.startsWith("Bearer ") ? List.of() : jdbcTemplate.query(
                "SELECT user_id, tenant_id, permissions FROM ops_api_token WHERE token_digest = ? AND revoked = false",
                (rs, i) -> new ApiCaller(rs.getString(1), rs.getString(2), Set.of(rs.getString(3).split(","))),
                Sha256Tool.sha256(header.substring(7).trim()));
        if (callers.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        ApiCaller caller = callers.get(0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                caller, null, caller.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
