package com.example.lending.ops.part5.jobs;

import java.time.Duration;

/** Tracks live console sessions per login category (staff, agent, partner). */
public interface SessionManager {

    int terminateIdle(Integer category, Duration idleFor);
}
