package com.acme.warehouse.web;

import com.acme.warehouse.service.BinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bins")
public class BinController {

    public record BinRequest(@NotBlank String code, @NotBlank String zone, @Min(1) int capacity) {
    }

    public record PutawayRequest(@Min(1) int units) {
    }

    public record ConsolidateRequest(@NotBlank String fromBin, @NotBlank String toBin) {
    }

    private final BinService bins;

    public BinController(BinService bins) {
        this.bins = bins;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody BinRequest request) {
        boolean created = bins.register(request.code(), request.zone(), request.capacity());
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @PostMapping("/{code}/putaway")
    public Map<String, Integer> putaway(@PathVariable String code, @Valid @RequestBody PutawayRequest request) {
        return Map.of("usedUnits", bins.recordPutaway(code, request.units()));
    }

    @PostMapping("/consolidate")
    public ResponseEntity<Void> consolidate(@Valid @RequestBody ConsolidateRequest request) {
        bins.consolidate(request.fromBin(), request.toBin());
        return ResponseEntity.noContent().build();
    }
}
