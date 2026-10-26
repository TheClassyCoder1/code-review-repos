package com.example.lending.ops.session;

import com.example.lending.ops.session.SessionSnapshotStore.ConsoleSessionSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.ObjectInputFilter;

/**
 * The only place a DefaultSerializer is created. Snapshots are read back through a strict
 * allow-list (the snapshot record plus the JDK types it is built from) with bounded graph size;
 * every other class is rejected before it is instantiated.
 */
@Configuration
public class SessionSnapshotConfig {

    static final String SNAPSHOT_FILTER = "maxdepth=8;maxrefs=1000;maxbytes=65536;"
            + "com.example.lending.ops.session.SessionSnapshotStore$ConsoleSessionSnapshot;java.lang.String;"
            + "java.lang.Record;java.util.ImmutableCollections$*;java.util.CollSer;java.util.ArrayList;!*";

    @Bean
    public DefaultSerializer<ConsoleSessionSnapshot> sessionSnapshotSerializer() {
        return new DefaultSerializer<>(ObjectInputFilter.Config.createFilter(SNAPSHOT_FILTER));
    }
}
