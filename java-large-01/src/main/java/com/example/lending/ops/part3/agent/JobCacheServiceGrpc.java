package com.example.lending.ops.part3.agent;

import com.example.lending.ops.part3.agent.JobCacheModel.*;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client side of the agent's job-cache endpoint (JSON transcoding of the gRPC service). */
public final class JobCacheServiceGrpc {
    private JobCacheServiceGrpc() { }
    public record JobCacheRequest(long jobId, String payload) {
        public static JobCacheRequest newBuilder() { return new JobCacheRequest(0, null); }
        public JobCacheRequest setJobId(long id) { return new JobCacheRequest(id, payload); }
        public JobCacheRequest setPayload(String p) { return new JobCacheRequest(jobId, p); }
        public JobCacheRequest build() { return this; }
    }
    public static class JobCacheServiceBlockingStub {
        private static final HttpClient HTTP = HttpClient.newHttpClient();
        private final URI saveUri;
        public JobCacheServiceBlockingStub(String host, int port) { saveUri = URI.create("http://" + host + ":" + port + "/job-cache/v1/save"); }
        public JobCacheReply save(JobCacheRequest request) {
            try {
                HttpRequest post = HttpRequest.newBuilder(saveUri).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.writeAsString(request))).build();
                return JsonUtils.readFromString(HTTP.send(post, HttpResponse.BodyHandlers.ofString()).body(), JobCacheReply.class);
            } catch (Exception e) {
                throw new IllegalStateException("job cache push to " + saveUri + " failed", e);
            }
        }
    }
    public static final class GrpcClient {
        private static final Map<String, JobCacheServiceBlockingStub> STUBS = new ConcurrentHashMap<>();
        public static <T> T getBlockingStub(String host, Integer port, Class<T> type) {
            return type.cast(STUBS.computeIfAbsent(host + ":" + port, k -> new JobCacheServiceBlockingStub(host, port)));
        }
    }
}
