package com.example.lending.ops.part5.realm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Agent account held in memory by {@link AgentAccountRealm}. {@link #toString()}
 * renders the internal account id only, so it is safe in messages and logs.
 */
@Getter
@RequiredArgsConstructor
public class SimpleAccount implements AuthenticationInfo {

    private final long accountId;
    private final String username;
    private final String credentials;
    @Setter
    private boolean locked;
    @Setter
    private boolean credentialsExpired;

    @Override
    public Object getPrincipals() {
        return accountId;
    }

    @Override
    public String toString() {
        return "agent#" + accountId;
    }
}
