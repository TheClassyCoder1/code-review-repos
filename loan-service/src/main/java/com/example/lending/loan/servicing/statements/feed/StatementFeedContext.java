package com.example.lending.loan.servicing.statements.feed;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Feed request: the feed name, the base URL of the portal and per-borrower feed variables. */
public class StatementFeedContext {

    private final String feedName;
    private final String baseUrl;
    private final Map<String, String> variables;

    public StatementFeedContext(String feedName, String baseUrl, Map<String, String> variables) {
        this.feedName = feedName;
        this.baseUrl = baseUrl;
        this.variables = Map.copyOf(variables);
    }

    public String getFeedName() { return feedName; }
    public String getVariable(String name) { return variables.get(name); }

    public String getViewURL(String name) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path("/statements/{name}").buildAndExpand(name).toUriString();
    }

    public String getURL(String context, String name, String query) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path("/{context}/{name}").query(query)
                .buildAndExpand(context, name).toUriString().replace("&", "&amp;");
    }
}
