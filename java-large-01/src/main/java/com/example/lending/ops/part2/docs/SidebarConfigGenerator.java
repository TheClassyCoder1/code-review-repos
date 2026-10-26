package com.example.lending.ops.part2.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** Regenerates the ops runbook site's sidebar: the published sidebar plus the runbooks in this build. */
public class SidebarConfigGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String ExistingSidebarsUrl;

    public SidebarConfigGenerator(String existingSidebarsUrl) {
        this.ExistingSidebarsUrl = existingSidebarsUrl;
    }

    public void generate(List<String> runbookPageIds, Path output) throws IOException {
        ObjectNode sidebars = (ObjectNode) MAPPER.readTree(loadExistingSidebar());
        runbookPageIds.stream().sorted().forEach(sidebars.with("docs").putArray("Ops console")::add);
        Files.writeString(output, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(sidebars));
    }

  private String loadExistingSidebar() throws IOException {
    return Request
        .Get(ExistingSidebarsUrl)
        .execute()
        .returnContent()
        .asString();
  }

    /** Fluent HTTP GET in the style of the Apache fluent client; as there, no timeout applies unless set. */
    record Request(String url, int timeoutMillis) {
        static Request Get(String url) { return new Request(url, 0); }
        Request timeout(int millis) { return new Request(url, millis); }

        Content execute() throws IOException {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
            try (InputStream in = conn.getInputStream()) {
                return new Content(in.readAllBytes());
            }
        }
    }

    record Content(byte[] body) {
        Content returnContent() { return this; }
        String asString() { return new String(body, StandardCharsets.UTF_8); }
    }
}
