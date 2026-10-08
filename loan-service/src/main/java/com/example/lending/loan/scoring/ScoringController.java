package com.example.lending.loan.scoring;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk")
public class ScoringController {

    private final ScenarioService scenarioService;
    private final ScorecardService scorecardService;

    public ScoringController(ScenarioService scenarioService, ScorecardService scorecardService) {
        this.scenarioService = scenarioService;
        this.scorecardService = scorecardService;
    }

    @PostMapping(value = "/scenarios/restore", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public RiskScenario restoreScenario(@RequestBody byte[] snapshot) {
        return scenarioService.restore(snapshot);
    }

    @GetMapping("/scenarios/active")
    public ResponseEntity<RiskScenario> activeScenario() {
        return ResponseEntity.of(scenarioService.activeScenario());
    }

    @PostMapping(value = "/scorecards", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ScorecardService.ScorecardSummary importScorecard(@RequestBody byte[] snapshot) {
        return scorecardService.importSnapshot(snapshot);
    }

    @GetMapping("/scorecards/grade")
    public GradeView grade(@RequestParam int score) {
        return new GradeView(score, scorecardService.grade(score));
    }

    public record GradeView(int score, String grade) {
    }
}
