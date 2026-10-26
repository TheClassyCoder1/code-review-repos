package com.example.lending.ops.part3.eventbus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.List;

/** Talks to the master nodes of the event-bus clusters that carry loan and repayment events. */
@Service
public class TubeMasterService {
    private final RestTemplate restTemplate =
            new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(10)).build();
    private final List<String> masterHosts;
    public TubeMasterService(@Value("${ops.eventbus.master-hosts:localhost:8080}") List<String> masterHosts) { this.masterHosts = masterHosts; }
    public String getQueryCountUrl(Integer clusterId, String method) {
        if (clusterId == null || clusterId < 0 || clusterId >= masterHosts.size()) throw new IllegalArgumentException("unknown event-bus cluster " + clusterId);
        return "http://" + masterHosts.get(clusterId) + "/webapi.htm?type=op_query&method=" + method + "&withCount=true";
    }
    public String queryMaster(String url) { return restTemplate.getForObject(url, String.class); }
    public static final class TubeConst { public static final String QUERY_CONSUMER_GROUP_INFO = "admin_query_consumer_group_info"; }
}
