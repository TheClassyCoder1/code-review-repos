package com.example.lending.ops.part5.realm;

/** The account's password is past its rotation date and must be reset first. */
public class ExpiredCredentialsException extends AuthenticationException {
    public ExpiredCredentialsException(String message) {
        super(message);
    }
}
