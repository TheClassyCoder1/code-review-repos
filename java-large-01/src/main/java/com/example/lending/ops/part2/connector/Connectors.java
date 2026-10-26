package com.example.lending.ops.part2.connector;

import java.util.List;
import java.util.Properties;

/** Plug-in points of the ops event bus (repayment reminders, collection escalations, agent alerts). */
public interface Connectors {

    interface CloudEvent { CloudEventData getData(); }

    interface CloudEventData { byte[] toBytes(); }

    interface Sink {
        void init(Properties props);
        void put(List<CloudEvent> events);
    }

    interface Source {
        void init(Properties props);
        byte[] poll() throws InterruptedException;
    }
}
