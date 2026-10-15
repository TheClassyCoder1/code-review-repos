package org.mercyhealth.lab.model;

public record Physician(long id, String email, String fullName, String role, String passwordHash) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
