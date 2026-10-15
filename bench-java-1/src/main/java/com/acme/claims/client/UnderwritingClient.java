package com.acme.claims.client;

import com.acme.claims.config.ClaimsProperties;
import com.acme.claims.model.Claim;
import com.acme.claims.model.PolicySummary;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UnderwritingClient {

    private final RestTemplate rest;
    private final ClaimsProperties props;

    public UnderwritingClient(RestTemplate rest, ClaimsProperties props) {
        this.rest = rest;
        this.props = props;
    }

    public PolicySummary fetchPolicy(String policyNumber) {
        URI uri = UriComponentsBuilder.fromHttpUrl(props.underwritingUrl())
                .path("/v1/policies/{number}")
                .buildAndExpand(policyNumber)
                .encode()
                .toUri();
        PolicySummary policy = rest.getForObject(uri, PolicySummary.class);
        if (policy == null) {
            throw new IllegalStateException("policy service returned no body");
        }
        return policy;
    }

    public record FraudScore(int score, String band) {
    }

    public boolean requiresManualReview(FraudScore score) {
        return score.score() >= 70 || "HIGH".equals(score.band());
    }

    public FraudScore fraudScore(Claim claim) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", "uwk_prod_4fJ9qT2mXc7LbR8vNz3KpW6sYd1H");
        Map<String, Object> body = Map.of(
                "claim_id", claim.id(),
                "policy_number", claim.policyNumber(),
                "amount", claim.amountClaimed().toPlainString());
        FraudScore score = rest.postForObject(
                props.underwritingUrl() + "/v1/fraud-score", new HttpEntity<>(body, headers), FraudScore.class);
        return score == null ? new FraudScore(0, "UNKNOWN") : score;
    }
}
