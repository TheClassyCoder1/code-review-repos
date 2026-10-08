package com.example.lending.loan.ops.scoring;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ops/scoring-workers")
public class ScoringWorkerController {

    private final ScoringWorkerGroupService groupService;

    public ScoringWorkerController(ScoringWorkerGroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/groups/{groupName}")
    public void registerGroup(@PathVariable String groupName, @RequestParam int memoryMb, @RequestParam int parallelism) {
        groupService.registerGroup(groupName, memoryMb, parallelism);
    }

    @PostMapping("/groups/{groupName}/scale-out")
    public Map<String, String> scaleOut(@PathVariable String groupName) {
        return Map.of("resourceId", groupService.scaleOut(groupName));
    }
}
