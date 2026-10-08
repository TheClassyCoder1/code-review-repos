package com.example.lending.loan.document.search;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SearchIndexMigrationRunner {

    private static final Duration MIGRATION_DEADLINE = Duration.ofMinutes(15);

    private final String esAddress;

    public SearchIndexMigrationRunner(@Value("${document.search.address}") String esAddress) {
        this.esAddress = esAddress;
    }

    public void migrate() throws Exception {
        MigrationContext context = new MigrationContext(UUID.randomUUID().toString());
        Instant deadline = Instant.now().plus(MIGRATION_DEADLINE);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(90))
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .addRequestInterceptorFirst((request, entity, httpContext) -> {
                    if (Instant.now().isAfter(deadline)) {
                        throw new IOException("Search index migration exceeded its deadline");
                    }
                })
                .build()) {
            SearchIndexMigration.waitForYellowStatus(httpClient, esAddress, context);
            context.printMessage("Search cluster ready, starting index migration");
        }
    }
}
