package com.example.lending.loan.ops.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ConsoleReportService {

    public String readRestrictedCall() {
        requireAuthority("read");
        return "portfolio-summary";
    }

    public void writeRestrictedCall() {
        requireAuthority("write");
    }

    private static void requireAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean granted = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
        if (!granted) {
            throw new AccessDeniedException("Missing authority " + authority);
        }
    }
}
