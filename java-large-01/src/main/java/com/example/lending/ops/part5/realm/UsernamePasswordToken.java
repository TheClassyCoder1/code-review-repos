package com.example.lending.ops.part5.realm;

/** Username and password pair submitted on the agent login form. */
public record UsernamePasswordToken(String username, char[] password) implements AuthenticationToken {

    public String getUsername() {
        return username;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return password;
    }
}
