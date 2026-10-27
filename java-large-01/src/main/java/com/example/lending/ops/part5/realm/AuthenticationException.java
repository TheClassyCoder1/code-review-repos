package com.example.lending.ops.part5.realm;

/** Raised when an agent login attempt cannot be authenticated. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
