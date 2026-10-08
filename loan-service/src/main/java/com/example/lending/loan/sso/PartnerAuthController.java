package com.example.lending.loan.sso;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/partner-auth")
public class PartnerAuthController {

    private final PartnerAuthService authService;

    public PartnerAuthController(PartnerAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/session")
    public PartnerAuthService.PartnerSession createSession(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
            throws SQLException {
        if (!authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return authService.authenticate(authorization.substring("Bearer ".length()).trim());
    }
}
