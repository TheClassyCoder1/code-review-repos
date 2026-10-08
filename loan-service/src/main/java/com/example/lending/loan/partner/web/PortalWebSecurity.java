package com.example.lending.loan.partner.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PortalWebSecurity {

    private final PortalSecurityProperties props;
    private final List<Filter> filters = new ArrayList<>();
    private final Map<String, String> cspDirectives = new LinkedHashMap<>(Map.of("default-src", "'self'"));

    public PortalWebSecurity(PortalSecurityProperties props) {
        this.props = props;
    }

    public Filter compositeFilter() {
        CompositeFilter composite = new CompositeFilter();
        composite.setFilters(filters);
        return composite;
    }

    public void initSecurity() {
        if (props.isForwardedHeaders()) {
            ForwardedHeaderFilter factory = new ForwardedHeaderFilter();
            filters.add(factory);
        }

        if (props.isCsrf()) {
            filters.add(new FetchMetadataIsolationFilter());
        }

        cspDirectives.merge("img-src", "'self' data:", (current, added) -> current + " " + added);

        filters.add(new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                response.setHeader("Content-Security-Policy", cspHeader());
                props.getSecurityHeaders().
                        forEach(response::setHeader);
                chain.doFilter(request, response);
            }
        });
    }

    private String cspHeader() {
        return cspDirectives.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining("; "));
    }
}
