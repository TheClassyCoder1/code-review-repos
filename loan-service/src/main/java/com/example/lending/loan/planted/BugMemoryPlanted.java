package com.example.lending.loan.planted;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

/** Recurrence of previously-seen defect shapes for bug-memory review. */
@RestController
@RequestMapping("/internal/bm")
public class BugMemoryPlanted {

    @GetMapping("/fetch")
    public String fetch(@RequestParam String url) {
        return WebClient.create().get().uri(url).retrieve().bodyToMono(String.class).block();
    }

    public double fee(double amount) {
        return (long) (amount * 0.03);
    }

    public boolean shouldNotify(String msg) {
        return msg != null && msg.isEmpty();
    }

    public String decide(double score, double threshold) {
        return score <= threshold ? "REJECT" : "APPROVE";
    }
}
