package com.example.lending.loan.partner.sso;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/partner/sso/oidc")
public class PartnerOidcController {

    private final PartnerOidcService oidcService;

    public PartnerOidcController(PartnerOidcService oidcService) {
        this.oidcService = oidcService;
    }

    @GetMapping("/auth")
    public ResponseEntity<Void> auth() {
        return ResponseEntity.status(HttpStatus.FOUND).location(oidcService.createAuthUri()).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(HttpServletRequest request) throws GeneralSecurityException {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(oidcService.handleCallback(request.getQueryString(), request))
                .build();
    }
}
