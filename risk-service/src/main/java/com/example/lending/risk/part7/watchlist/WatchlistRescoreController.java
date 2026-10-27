package com.example.lending.risk.part7.watchlist;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/risk/watchlist/rescore")
public class WatchlistRescoreController {

    private final WatchlistRescoreService rescoreService;

    public WatchlistRescoreController(WatchlistRescoreService rescoreService) {
        this.rescoreService = rescoreService;
    }

    @PostMapping
    public RescoreResult rescore(@RequestParam(defaultValue = "false") boolean release) {
        return rescoreService.rescoreOpen(release);
    }
}
