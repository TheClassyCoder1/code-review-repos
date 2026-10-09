package com.example.lending.loan.servicing.admin.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Read-only queries against the statement search cluster. */
public final class SearchIndexInspector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SearchIndexInspector() {
    }

    public static Set<String> getIndexesPrefixedBy(CloseableHttpClient httpClient, String esAddress, String prefix) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(esAddress + "/_aliases"))) {
            if (response.getCode() == HttpStatus.SC_OK) {
                JsonNode indexesAsJson = MAPPER.readTree(EntityUtils.toString(response.getEntity()));
                Set<String> indexes = new TreeSet<>();
                indexesAsJson.fieldNames().forEachRemaining(alias -> {
                    if (alias.startsWith(prefix)) {
                        indexes.add(alias);
                    }
                });
                return indexes;
            }
        } catch (ParseException e) {
            throw new IOException("Unreadable response from search cluster", e);
        }
        return Collections.emptySet();
    }
}
