package com.example.lending.ops.part3.batch;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.HashMap;

/** ResourceManager REST client for the batch cluster running nightly scoring jobs; keeps the init/start lifecycle. */
public class YarnClient {
    private volatile Service.STATE state = Service.STATE.NOTINITED;
    private String resourceManagerUrl;
    private RestTemplate restTemplate;
    public static YarnClient createYarnClient() { return new YarnClient(); }
    public void init(Configuration conf) { resourceManagerUrl = conf.getOrDefault(YarnConfiguration.RM_WEBAPP_ADDRESS, "http://localhost:8088"); state = Service.STATE.INITED; }
    public void start() {
        restTemplate = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(20)).build();
        state = Service.STATE.STARTED;
    }
    public boolean isInState(Service.STATE expected) { return state == expected; }
    public String getApplicationState(String applicationId) {
        JsonNode app = restTemplate.getForObject(resourceManagerUrl + "/ws/v1/cluster/apps/{id}", JsonNode.class, applicationId);
        return app == null ? "UNKNOWN" : app.path("app").path("state").asText("UNKNOWN");
    }
    public interface Service { enum STATE { NOTINITED, INITED, STARTED, STOPPED } }
    public static class Configuration extends HashMap<String, String> { }
    public static class YarnConfiguration extends Configuration {
        public static final String RM_WEBAPP_ADDRESS = "yarn.resourcemanager.webapp.address";
        public YarnConfiguration(Configuration base) { putAll(base); }
    }
}
