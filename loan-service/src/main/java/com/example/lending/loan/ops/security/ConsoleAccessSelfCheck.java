package com.example.lending.loan.ops.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

public class ConsoleAccessSelfCheck implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAccessSelfCheck.class);

    private final AuthenticationManager authenticationManager;
    private final ConsoleReportService reportService;

    public ConsoleAccessSelfCheck(AuthenticationManager authenticationManager, ConsoleReportService reportService) {
        this.authenticationManager = authenticationManager;
        this.reportService = reportService;
    }

    @Override
    public void run(String... args) {

        // Subject is not authenticated yet
        Assert.isTrue(SecurityContextHolder.getContext().getAuthentication() == null, "context must start empty");

        // login the subject with a username / password
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated("joe.coder", "password");
        Authentication subject = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(subject);

        // joe.coder has the "user" role
        Assert.isTrue(hasAuthority(subject, "ROLE_USER"), "joe.coder must have the user role");

        // joe.coder does NOT have the admin role
        Assert.isTrue(!hasAuthority(subject, "ROLE_ADMIN"), "joe.coder must not have the admin role");

        // joe.coder has the "read" permission
        Assert.isTrue(hasAuthority(subject, "read"), "joe.coder must have the read permission");

        // current user is allowed to execute this method.
        reportService.readRestrictedCall();

        try {
            // but not this one!
            reportService.writeRestrictedCall();
        } catch (AccessDeniedException e) {
            log.info("Subject was not allowed to execute method 'writeRestrictedCall'");
        }

        // logout
        SecurityContextHolder.clearContext();
        Assert.isTrue(SecurityContextHolder.getContext().getAuthentication() == null, "context must be cleared");
    }

    private static boolean hasAuthority(Authentication subject, String authority) {
        return subject.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }
}
