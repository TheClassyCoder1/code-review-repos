package com.example.lending.loan.servicing.admin.imports;

import java.util.ArrayList;
import java.util.List;

/** Operator record of an import file. */
public class OperatorRepresentation {

    /** Credential in an import file: either a plain value to be hashed or a pre-hashed secret. */
    public static class CredentialRepresentation {

        public static final String PASSWORD = "password";

        private String id;
        private String type;
        private String value;
        private String secretData;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getSecretData() { return secretData; }
        public void setSecretData(String secretData) { this.secretData = secretData; }
    }

    private String username;
    private String email;
    private String displayName;
    private String password;
    private List<CredentialRepresentation> credentials;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /** Old import format: a single plain password field. */
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<CredentialRepresentation> getCredentials() { return credentials; }
    public void setCredentials(List<CredentialRepresentation> credentials) { this.credentials = credentials; }

    public void addCredential(CredentialRepresentation credential) {
        if (credentials == null) {
            credentials = new ArrayList<>();
        }
        credentials.add(credential);
    }
}
