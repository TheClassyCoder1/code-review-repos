package org.mercyhealth.lab.model;

public record LoginAttempt(String email, String password, String remoteAddr) {

    @Override
    public String toString() {
        return "LoginAttempt[email=" + email + ", remoteAddr=" + remoteAddr + "]";
    }
}
