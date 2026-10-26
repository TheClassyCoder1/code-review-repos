package com.example.lending.risk.part7.watchlist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/risk/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<WatchlistEntry> add(@RequestBody WatchlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchlistService.add(request));
    }

    @GetMapping
    public List<WatchlistEntry> open(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return watchlistService.open(page, size);
    }

    @GetMapping("/summary")
    public Map<WatchlistEntry.Reason, Long> summary() {
        return watchlistService.openCountsByReason();
    }

    @GetMapping("/loans/{loanId}")
    public List<WatchlistEntry> history(@PathVariable Long loanId) {
        return watchlistService.history(loanId);
    }

    @DeleteMapping("/loans/{loanId}")
    public WatchlistEntry release(@PathVariable Long loanId, @RequestParam(required = false) String note) {
        return watchlistService.release(loanId, note);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
