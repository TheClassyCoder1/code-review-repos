package com.example.lending.loan.servicing.admin.imports;

/** Rejected operator data. */
public class ModelException extends RuntimeException {

    private Object[] parameters;

    public ModelException(String message, Object... parameters) {
        super(message);
        this.parameters = parameters;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    /** Imported password does not satisfy the tenant password policy. */
    public static class PasswordPolicyNotMetException extends ModelException {

        private final String username;

        public PasswordPolicyNotMetException(String message, String username, Throwable cause) {
            super(message);
            initCause(cause);
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }
}
