package com.example.lending.loan.document.lock;

import java.time.Instant;

/** Lock held on a loan document while it is being edited or countersigned. */
public class DocumentLock {

    public enum Type { READ, WRITE }

    public enum Scope { EXCLUSIVE, SHARED }

    private final WriteLockEntry entry;
    private final String owner;
    private final Instant expiresAt;

    public DocumentLock(Scope scope, String owner, Instant expiresAt) {
        this.entry = new WriteLockEntry(Type.WRITE, scope);
        this.owner = owner;
        this.expiresAt = expiresAt;
    }

    public Scope getScope() { return entry.scope; }
    public String getOwner() { return owner; }
    public Instant getExpiresAt() { return expiresAt; }

    private static final class WriteLockEntry {

        private final Scope scope;

        WriteLockEntry(Type type, Scope scope) {
            if (!Type.WRITE.equals(type)) {
                throw new IllegalArgumentException("Invalid Type:" + type);
            }
            if (!Scope.EXCLUSIVE.equals(scope) && !Scope.SHARED.equals(scope)) {
                throw new IllegalArgumentException("Invalid scope:" +scope);
            }
            this.scope = scope;
        }
    }
}
