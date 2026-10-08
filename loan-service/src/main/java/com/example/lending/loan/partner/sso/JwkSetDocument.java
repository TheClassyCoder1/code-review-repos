package com.example.lending.loan.partner.sso;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/** Public view of a JWK set: private key members are dropped before rendering. */
final class JwkSetDocument {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> PRIVATE_MEMBERS = List.of("d", "p", "q", "dp", "dq", "qi", "oth", "k");

    private final JsonNode jwkSet;

    JwkSetDocument(String json) {
        try {
            this.jwkSet = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JWK set", e);
        }
        for (JsonNode key : jwkSet.path("keys")) {
            if (key instanceof ObjectNode objectNode) {
                objectNode.remove(PRIVATE_MEMBERS);
            }
        }
    }

    String toString(String mediaType) {
        if ("xml".equalsIgnoreCase(mediaType)) {
            return "<jwks>" + HtmlUtils.htmlEscape(jwkSet.toString()) + "</jwks>";
        }
        return jwkSet.toString();
    }
}
