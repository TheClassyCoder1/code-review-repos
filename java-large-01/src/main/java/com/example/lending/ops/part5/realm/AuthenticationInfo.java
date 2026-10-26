package com.example.lending.ops.part5.realm;

/** Stored account data the credentials matcher compares a token against. */
public interface AuthenticationInfo {
    Object getPrincipals();

    Object getCredentials();
}
