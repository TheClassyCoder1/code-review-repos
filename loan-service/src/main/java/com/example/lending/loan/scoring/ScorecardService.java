package com.example.lending.loan.scoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ScorecardService {

    private static final Logger log = LoggerFactory.getLogger(ScorecardService.class);
    private static final String UNRATED = "UNRATED";

    private final ScorecardStreams scorecardStreams;
    private final AtomicReference<ScorecardSnapshot> active = new AtomicReference<>();

    public ScorecardService(ScorecardStreams scorecardStreams) {
        this.scorecardStreams = scorecardStreams;
    }

    public ScorecardSummary importSnapshot(byte[] payload) {
        ScorecardSnapshot snapshot;
        try (ObjectInputStream in = scorecardStreams.open(payload)) {
            snapshot = (ScorecardSnapshot) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scorecard snapshot could not be read");
        }
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scorecard snapshot is empty");
        }
        requireDisjoint(snapshot.bands());
        active.set(snapshot);
        log.info("Activated scorecard with {} bands", snapshot.bands().length);
        return new ScorecardSummary(snapshot.version(), snapshot.bands().length);
    }

    public String grade(int score) {
        ScorecardSnapshot snapshot = active.get();
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No scorecard is active");
        }
        for (ScoreBand band : snapshot.bands()) {
            if (band.contains(score)) {
                return band.grade();
            }
        }
        return UNRATED;
    }

    private static void requireDisjoint(ScoreBand[] bands) {
        ScoreBand[] sorted = bands.clone();
        Arrays.sort(sorted, Comparator.comparingInt(ScoreBand::minScore));
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].minScore() <= sorted[i - 1].maxScore()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scorecard bands overlap");
            }
        }
    }

    public record ScorecardSummary(String version, int bandCount) {
    }
}
