package com.example.lending.loan.servicing.settlement.oauth;

import com.example.lending.loan.servicing.settlement.oauth.SettlementOAuthTokenService.OAuthAccessor;
import com.example.lending.loan.servicing.settlement.oauth.SettlementOAuthTokenService.OAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** OAuth 1.0a request token endpoint for legacy settlement partners. */
@RestController
@RequestMapping("/servicing/settlement/oauth")
public class SettlementOAuthController {

    private final SettlementOAuthTokenService tokenService;

    public SettlementOAuthController(SettlementOAuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/request-token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> requestToken(@RequestParam("oauth_consumer_key") String consumerKey) {
        try {
            OAuthAccessor accessor = new OAuthAccessor(tokenService.getConsumer(consumerKey));
            tokenService.generateRequestToken(accessor);
            return ResponseEntity.ok("oauth_token=" + encode(accessor.requestToken)
                    + "&oauth_token_secret=" + encode(accessor.tokenSecret)
                    + "&oauth_callback_confirmed=true");
        } catch (OAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("oauth_problem=" + encode(e.getMessage()));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
