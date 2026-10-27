package com.acme.warehouse.web;

import com.acme.warehouse.model.CountRequest;
import com.acme.warehouse.service.CycleCountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/counts")
public class CycleCountController {

    private final CycleCountService counts;

    public CycleCountController(CycleCountService counts) {
        this.counts = counts;
    }

    @PostMapping
    public Map<String, Long> open(@Valid @RequestBody CountRequest request) {
        return Map.of("sessionId", counts.openSession(request));
    }

    @PutMapping("/{sessionId}/lines/{sku}")
    public void recordLine(@PathVariable long sessionId, @PathVariable String sku,
                           @RequestParam @Min(0) int counted) {
        counts.recordLine(sessionId, sku, counted);
    }

    @PostMapping("/{sessionId}/post")
    public CycleCountService.PostResult post(@PathVariable long sessionId) {
        return counts.postVariances(sessionId);
    }

    @PostMapping("/{sessionId}/lines/{sku}/apply")
    public Map<String, Integer> apply(@PathVariable long sessionId, @PathVariable String sku) {
        return Map.of("quantity", counts.applyVariance(sessionId, sku));
    }

    @GetMapping("/{sessionId}/summary")
    public CycleCountService.CountSummary summary(@PathVariable long sessionId) {
        return counts.summarize(sessionId);
    }
}
