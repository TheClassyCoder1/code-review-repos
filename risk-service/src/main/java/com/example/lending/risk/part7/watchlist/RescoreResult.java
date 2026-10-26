package com.example.lending.risk.part7.watchlist;

import java.util.List;

/** Outcome of a watchlist re-score run, items ordered by largest score increase first. */
public record RescoreResult(int reviewed, int released, List<Item> items) {

    public record Item(Long entryId, Long loanId, WatchlistEntry.Reason reason, double scoreAtEntry,
                       double currentScore, double drift, boolean released) {
    }
}
