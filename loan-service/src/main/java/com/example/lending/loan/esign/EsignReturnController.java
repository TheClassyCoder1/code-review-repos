package com.example.lending.loan.esign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Browser landing point after the borrower finishes signing on the e-sign provider's page. */
@RestController
public class EsignReturnController {

    private final String defaultReturnUrl;

    public EsignReturnController(@Value("${lending.portal.base-url}") String portalBaseUrl) {
        this.defaultReturnUrl = portalBaseUrl + "/loans";
    }

    @GetMapping("/api/v1/esign/return")
    public ResponseEntity<Void> signingReturn(@RequestParam(required = false) String returnUrl) {
        String target = (returnUrl == null || returnUrl.isBlank()) ? defaultReturnUrl : returnUrl;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
