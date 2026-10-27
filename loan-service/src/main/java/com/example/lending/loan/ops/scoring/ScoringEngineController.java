package com.example.lending.loan.ops.scoring;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ops/scoring-engines")
public class ScoringEngineController {

    @PostMapping("/inspect")
    public Map<String, String> inspect(@RequestParam String engineHome) {
        ScoringEngineVersion engine = new ScoringEngineVersion(engineHome);
        return Map.of("engineHome", engine.getEngineHome(),
                "version", engine.getVersion(),
                "modelFormat", engine.getModelFormat());
    }
}
