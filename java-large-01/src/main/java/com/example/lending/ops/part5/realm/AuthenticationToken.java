package com.example.lending.ops.part5.realm;

/** Credentials submitted by a console agent. */
public interface AuthenticationToken {
    Object getPrincipal();

    Object getCredentials();
}
