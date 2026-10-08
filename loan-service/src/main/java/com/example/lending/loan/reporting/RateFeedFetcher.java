package com.example.lending.loan.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;

@Component
public class RateFeedFetcher {

    public record RateFeed(String source, String currency, BigDecimal rate, LocalDate asOf) {
    }

    private static final Logger log = LoggerFactory.getLogger(RateFeedFetcher.class);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final HttpRequest.Builder requestBuilder =
            HttpRequest.newBuilder().timeout(Duration.ofSeconds(15)).header("Accept", "application/json").GET();
    private final ObjectMapper mapper;

    public RateFeedFetcher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<RateFeed> fetchAll(List<String> urls) throws InterruptedException {
        List<RateFeed> feeds = new ArrayList<>();
        for (String url : urls) {
            try {
                feeds.add(fetchFeed(url));
            } catch (IOException e) {
                log.warn("Rate feed {} unavailable: {}", url, e.getClass().getSimpleName());
            }
        }
        return feeds;
    }

    private RateFeed fetchFeed(String url) throws IOException, InterruptedException {

        HttpRequest request = requestBuilder.copy().uri(URI.create(url)).build();

        try (InputStream body = client.send(request, ofInputStream()).body()) {
            return mapper.readValue(body, RateFeed.class);
        }

    }
}
