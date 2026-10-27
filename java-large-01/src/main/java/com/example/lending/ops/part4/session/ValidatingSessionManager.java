package com.example.lending.ops.part4.session;

/** A session manager that can sweep its store and expire sessions that are stopped or timed out. */
public interface ValidatingSessionManager {

    void validateSessions();
}
