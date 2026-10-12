package com.example.lending.loan.servicing.settlement.tokens;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/** Delegation token issued by the settlement gateway: identifier bytes, secret password, kind and service. */
public final class SettlementToken {

    private static final Map<String, Supplier<TokenIdentifier>> IDENTIFIERS = Map.of(
            DelegationTokens.DelegationTokenIdentifier.KIND, DelegationTokens.DelegationTokenIdentifier::new);

    private final byte[] identifier;
    private final byte[] password;
    private final String kind;
    private final String service;

    public SettlementToken(byte[] identifier, byte[] password, String kind, String service) {
        this.identifier = Arrays.copyOf(identifier, identifier.length);
        this.password = Arrays.copyOf(password, password.length);
        this.kind = kind;
        this.service = service;
    }

    public byte[] getIdentifier() {
        return Arrays.copyOf(identifier, identifier.length);
    }

    public byte[] getPassword() {
        return Arrays.copyOf(password, password.length);
    }

    public String getKind() {
        return kind;
    }

    public String getService() {
        return service;
    }

    /** Decodes the identifier for known kinds; returns {@code null} when the kind is not registered. */
    public TokenIdentifier decodeIdentifier() throws IOException {
        Supplier<TokenIdentifier> factory = IDENTIFIERS.get(kind);
        if (factory == null) {
            return null;
        }
        TokenIdentifier tokenIdentifier = factory.get();
        tokenIdentifier.readFields(new DataInputStream(new ByteArrayInputStream(identifier)));
        return tokenIdentifier;
    }

    @Override
    public String toString() {
        return "SettlementToken[kind=" + kind + ", service=" + service + "]";
    }
}
