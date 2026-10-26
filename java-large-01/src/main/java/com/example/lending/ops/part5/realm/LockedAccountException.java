package com.example.lending.ops.part5.realm;

/** The account exists but has been locked by an administrator or by repeated failures. */
public class LockedAccountException extends AuthenticationException {
    public LockedAccountException(String message) {
        super(message);
    }
}
