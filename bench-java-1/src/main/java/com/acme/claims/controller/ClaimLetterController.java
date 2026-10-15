package com.acme.claims.controller;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.security.ClaimAccess;
import com.acme.claims.security.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping(value = "/api/claims/{claimId}/letters", produces = MediaType.TEXT_HTML_VALUE)
public class ClaimLetterController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneOffset.UTC);

    private final ClaimAccess access;

    public ClaimLetterController(ClaimAccess access) {
        this.access = access;
    }

    @GetMapping("/acknowledgement")
    public String acknowledgement(@PathVariable long claimId, HttpServletRequest request) {
        Claim claim = access.requireOwned(claimId, RequestUser.from(request));
        return "<html><body><h1>Claim " + claim.id() + " received</h1>"
                + "<p>Filed on " + DATE.format(claim.createdAt()) + " under policy " + claim.policyNumber() + ".</p>"
                + "<p>What you told us: " + claim.description() + "</p>"
                + "<p>An adjuster will contact you within five business days.</p></body></html>";
    }

    private static String wrap(String title, String body) {
        return "<html><head><title>" + title + "</title></head>"
                + "<body>" + body + "</body></html>";
    }

    @GetMapping("/status-badge")
    public String statusBadge(@PathVariable long claimId, HttpServletRequest request) {
        Claim claim = access.requireOwned(claimId, RequestUser.from(request));
        ClaimStatus status = claim.status();
        return "<span class=\"badge badge-" + status.name().toLowerCase() + "\">" + status.label() + "</span>";
    }

    private static String signature() {
        return "<p>Sincerely,<br>Claims Department<br>"
                + "Acme Insurance</p>";
    }

    @GetMapping("/denial")
    public String denialNotice(@PathVariable long claimId, HttpServletRequest request) {
        Claim claim = access.requireOwned(claimId, RequestUser.from(request));
        if (claim.status() != ClaimStatus.DENIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "claim is not denied");
        }
        String body = "<p>We have reviewed claim " + claim.id() + " and are unable to approve it.</p>"
                + "<p>Claim summary: " + HtmlUtils.htmlEscape(claim.description()) + "</p>"
                + signature();
        return wrap("Claim decision", body);
    }
}
