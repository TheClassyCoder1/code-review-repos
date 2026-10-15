package com.acme.claims.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "claims")
public record ClaimsProperties(
        String storageRoot,
        String quarantineDir,
        String thumbnailDir,
        String portalBaseUrl,
        String underwritingUrl,
        String paymentGatewayUrl,
        String paymentGatewayApiKey,
        List<String> repairShopHosts) {
}
