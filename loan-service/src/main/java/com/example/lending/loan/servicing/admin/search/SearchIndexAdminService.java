package com.example.lending.loan.servicing.admin.search;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

/** Index housekeeping views for the statement search cluster. */
@Service
public class SearchIndexAdminService {

    private final CloseableHttpClient httpClient;
    private final String esAddress;

    public SearchIndexAdminService(@Qualifier("servicingHttpClient") CloseableHttpClient httpClient,
                                   @Value("${servicing.search.address}") String esAddress) {
        this.httpClient = httpClient;
        this.esAddress = esAddress;
    }

    public Set<String> statementIndexes(String prefix) throws IOException {
        return SearchIndexInspector.getIndexesPrefixedBy(httpClient, esAddress, prefix);
    }
}
