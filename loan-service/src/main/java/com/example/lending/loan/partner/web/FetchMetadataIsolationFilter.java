package com.example.lending.loan.partner.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Resource isolation based on Fetch Metadata request headers. */
class FetchMetadataIsolationFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String site = request.getHeader("Sec-Fetch-Site");
        boolean allowed = site == null
                || "same-origin".equals(site)
                || "none".equals(site)
                || ("navigate".equals(request.getHeader("Sec-Fetch-Mode")) && SAFE_METHODS.contains(request.getMethod()));
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
