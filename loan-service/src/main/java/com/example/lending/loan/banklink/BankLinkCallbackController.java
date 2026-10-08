package com.example.lending.loan.banklink;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Redirect target registered with the open-banking provider. */
@RestController
@RequestMapping("/api/v1/bank-link")
public class BankLinkCallbackController {

    private final BankLinkService bankLinkService;

    public BankLinkCallbackController(BankLinkService bankLinkService) {
        this.bankLinkService = bankLinkService;
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String state, @RequestParam String code) {
        BankLinkSession session = bankLinkService.complete(state, code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(session.getReturnUrl())).build();
    }
}
