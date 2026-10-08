package com.example.lending.loan.ops.monitor;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/ops/monitor/kafka")
public class KafkaMonitorController {

    private final String bootstrapServers;

    public KafkaMonitorController(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @GetMapping("/topics")
    public List<List<String>> topics(@RequestParam(defaultValue = "false") boolean internal)
            throws ExecutionException, InterruptedException {
        List<List<String>> rows = new ArrayList<>();
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15_000,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000))) {
            KafkaTopicCollector.collectTopicDescribe(rows, adminClient, internal);
        }
        return rows;
    }
}
