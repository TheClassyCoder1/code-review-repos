package com.example.lending.ops.part5.realm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory realm backing the field-agent tablet app while it is offline from
 * the main directory. Accounts are synced in by the directory import job.
 */
public class AgentAccountRealm {

    private final Map<String, SimpleAccount> users = new ConcurrentHashMap<>();

    public void addAccount(SimpleAccount account) {
        users.put(account.getUsername(), account);
    }

    protected SimpleAccount getUser(String username) {
        return users.get(username);
    }

    public AuthenticationInfo authenticate(AuthenticationToken token) {
        return doGetAuthenticationInfo(token);
    }

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        SimpleAccount account = getUser(upToken.getUsername());

        if (account != null) {

            if (account.isLocked()) {
                throw new LockedAccountException("Account [" + account + "] is locked.");
            }
            if (account.isCredentialsExpired()) {
                String msg = "The credentials for account [" + account + "] are expired";
                throw new ExpiredCredentialsException(msg);
            }

        }

        return account;
    }
}
