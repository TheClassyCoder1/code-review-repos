package com.example.lending.loan.servicing.recon.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Asks a ledger feed for selected properties of a collection. */
public class LedgerPropertiesRequest extends HttpGet {

    /** Property listing returned by a ledger feed for a collection. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MultiStatus(List<MultiStatusResponse> responses) {

        public List<MultiStatusResponse> getResponses() {
            return responses == null ? List.of() : responses;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record MultiStatusResponse(String href, Map<String, String> properties) {

            public Map<String, String> getProperties() {
                return properties == null ? Map.of() : properties;
            }
        }
    }

    public static final int DEPTH_0 = 0;

    private static final int MAX_BODY_CHARS = 1_000_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public LedgerPropertiesRequest(String path, Set<String> properties, int depth) {
        super(path + "?props=" + String.join(",", properties));
        setHeader("Depth", String.valueOf(depth));
        setHeader("Accept", "application/json");
    }

    public boolean succeeded(ClassicHttpResponse response) {
        int code = response.getCode();
        return code == HttpStatus.SC_OK || code == HttpStatus.SC_MULTI_STATUS;
    }

    public MultiStatus getResponseBodyAsMultiStatus(ClassicHttpResponse response) throws IOException {
        try {
            String body = EntityUtils.toString(response.getEntity(), MAX_BODY_CHARS);
            return MAPPER.readValue(body, MultiStatus.class);
        } catch (ParseException e) {
            throw new IOException("Unreadable ledger feed response", e);
        }
    }
}
