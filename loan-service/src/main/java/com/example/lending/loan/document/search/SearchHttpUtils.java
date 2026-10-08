package com.example.lending.loan.document.search;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Map;

final class SearchHttpUtils {

    private SearchHttpUtils() {
    }

    static String executeGetRequest(CloseableHttpClient httpClient, String url, Map<String, String> headers) throws IOException {
        HttpGet get = new HttpGet(url);
        if (headers != null) {
            headers.forEach(get::setHeader);
        }
        return httpClient.execute(get, response -> {
            if (response.getCode() >= 400) {
                throw new IOException("Search cluster returned HTTP " + response.getCode());
            }
            return EntityUtils.toString(response.getEntity());
        });
    }
}
