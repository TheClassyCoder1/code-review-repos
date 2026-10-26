package com.example.lending.ops.part3.eventbus;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.part3.eventbus.TubeMasterService.TubeConst;
import com.example.lending.ops.part3.support.Gson;
import com.example.lending.ops.part3.support.Gson.*;
import lombok.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Capacity overview of the event-bus clusters for the platform on-call dashboard. */
@RestController
@RequestMapping("/api/ops/event-bus/clusters")
@RequiredArgsConstructor
public class EventBusClusterController {
    private final TubeMasterService masterService;
    private final Gson gson = new Gson();
    @GetMapping("/{clusterId}/summary")
    @PreAuthorize("hasRole('PLATFORM_ONCALL')")
    public Result<Map<String, Integer>> summary(@PathVariable Integer clusterId) {
        return Result.OK(Map.of("consumerGroups", getConsumerGroupCount(clusterId)));
    }
    public int getConsumerGroupCount(Integer clusterId) {
        String queryUrl = masterService.getQueryCountUrl(clusterId, TubeConst.QUERY_CONSUMER_GROUP_INFO);
        int consumerGroupCount = 0;
        String groupData = masterService.queryMaster(queryUrl);
        JsonObject jsonObject = gson.fromJson(groupData, JsonObject.class);
        JsonElement data = jsonObject.get("data");
        JsonArray jsonData = gson.fromJson(data, JsonArray.class);
        List<ConsumerGroupInfoRes> groupList = gson.fromJson(jsonData.toString(),
                new TypeToken<List<ConsumerGroupInfoRes>>() {
                }.getType());
        for (ConsumerGroupInfoRes groupInfoRes : groupList) {
            consumerGroupCount = consumerGroupCount + (int) Math.ceil(groupInfoRes.getGroupCount());
        }
        return consumerGroupCount;
    }
    @Data
    public static class ConsumerGroupInfoRes { private String topicName; private double groupCount; }
}
