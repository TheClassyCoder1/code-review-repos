package com.example.lending.loan.document.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

public final class SearchIndexMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SearchIndexMigration() {
    }

    public static void waitForYellowStatus(CloseableHttpClient httpClient, String esAddress, MigrationContext migrationContext) throws Exception {
        while (true) {
            final JsonNode status = MAPPER.readTree(SearchHttpUtils.executeGetRequest(httpClient, esAddress + "/_cluster/health?wait_for_status=yellow&timeout=60s", null));
            if (!status.path("timed_out").asText().equals("true")) {
                migrationContext.printMessage("Search cluster status is " + status.path("status").asText());
                break;
            }
            migrationContext.printMessage("Waiting for search cluster status to be yellow, current status is " + status.path("status").asText());
        }

    }
}
