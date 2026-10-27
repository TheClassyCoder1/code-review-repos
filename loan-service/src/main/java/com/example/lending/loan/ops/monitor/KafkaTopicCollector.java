package com.example.lending.loan.ops.monitor;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

final class KafkaTopicCollector {

    private KafkaTopicCollector() {
    }

    static void collectTopicDescribe(List<List<String>> builder, AdminClient adminClient, Boolean monitorInternalTopic) throws InterruptedException, ExecutionException {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult listTopicsResult = adminClient.listTopics(options);
        Set<String> names = listTopicsResult.names().get();
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(names);
        Map<String, TopicDescription> topicDescriptionMap = describeTopicsResult.allTopicNames().get().entrySet().stream()
                .filter(entry -> filterInternalTopics(entry.getKey(), monitorInternalTopic))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        topicDescriptionMap.forEach((key, value) -> {
            List<TopicPartitionInfo> listp = value.partitions();
            listp.forEach(info -> {
                List<String> valueRowBuilder = new ArrayList<>();
                valueRowBuilder.add(value.name());
                valueRowBuilder.add(String.valueOf(value.partitions().size()));
                valueRowBuilder.add(String.valueOf(info.partition()));
                valueRowBuilder.add(info.leader().host());
                valueRowBuilder.add(String.valueOf(info.leader().port()));
                valueRowBuilder.add(String.valueOf(info.replicas().size()));
                valueRowBuilder.add(String.valueOf(info.replicas()));
                builder.add(valueRowBuilder);
            });
        });
    }

    private static boolean filterInternalTopics(String topic, Boolean monitorInternalTopic) {
        return Boolean.TRUE.equals(monitorInternalTopic) || !topic.startsWith("__");
    }
}
