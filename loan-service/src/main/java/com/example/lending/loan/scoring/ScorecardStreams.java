package com.example.lending.loan.scoring;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/** Opens serialized scorecard snapshots produced by the model training pipeline. */
@Component
public class ScorecardStreams {

    private static final ObjectInputFilter SCORECARD_CLASSES = ObjectInputFilter.Config.createFilter(
            "maxdepth=4;maxrefs=512;maxarray=64;maxbytes=65536;"
                    + "com.example.lending.loan.scoring.ScorecardSnapshot;"
                    + "com.example.lending.loan.scoring.ScoreBand;"
                    + "java.lang.String;"
                    + "!*");

    public ObjectInputStream open(byte[] payload) throws IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload));
        in.setObjectInputFilter(SCORECARD_CLASSES);
        return in;
    }
}
