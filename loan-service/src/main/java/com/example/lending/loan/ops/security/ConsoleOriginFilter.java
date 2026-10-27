package com.example.lending.loan.ops.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Rejects state-changing console requests that do not originate from the console itself.
 * Runs ahead of the Spring Security filter chain so sign-in and sign-out are covered too.
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
public class ConsoleOriginFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final String consoleOrigin;

    public ConsoleOriginFilter(@Value("${ops.console.origin}") String consoleOrigin) {
        this.consoleOrigin = consoleOrigin;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/ops/") || SAFE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = originOf(request.getHeader("Referer"));
        }
        if (!consoleOrigin.equals(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private static String originOf(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
