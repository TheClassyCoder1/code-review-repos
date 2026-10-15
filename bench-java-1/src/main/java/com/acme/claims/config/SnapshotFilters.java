package com.acme.claims.config;

import java.io.ObjectInputFilter;

public final class SnapshotFilters {

    public static final ObjectInputFilter DRAFT_FILTER = ObjectInputFilter.Config.createFilter(
            "com.acme.claims.model.DraftClaim;java.lang.*;java.math.BigDecimal;java.math.BigInteger;"
                    + "java.time.*;java.util.ArrayList;maxdepth=5;maxrefs=200;maxbytes=65536;!*");

    private SnapshotFilters() {
    }
}
