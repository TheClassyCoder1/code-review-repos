package com.example.lending.loan.servicing.settlement.tokens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

/** Helpers for settlement gateway delegation tokens. */
public final class DelegationTokens {

    private static final Logger LOG = LoggerFactory.getLogger(DelegationTokens.class);

    private DelegationTokens() {
    }

    /** Identifier of settlement gateway delegation tokens. */
    public static class DelegationTokenIdentifier extends AbstractDelegationTokenIdentifier {

        public static final String KIND = "SETTLEMENT_DELEGATION_TOKEN";

        @Override
        public String getKind() {
            return KIND;
        }
    }

    public static Optional<Long> getTokenIssueDate(SettlementToken token)
            throws IOException {
        TokenIdentifier identifier = token.decodeIdentifier();
        if (identifier instanceof AbstractDelegationTokenIdentifier) {
            return Optional.of(((AbstractDelegationTokenIdentifier) identifier).getIssueDate());
        }
        if (identifier == null) {
            // Kind not registered: try the default delegation token layout
            DelegationTokenIdentifier tokenIdentifier = new DelegationTokenIdentifier();
            ByteArrayInputStream buf = new ByteArrayInputStream(token.getIdentifier());
            DataInputStream in = new DataInputStream(buf);
            try {
                tokenIdentifier.readFields(in);
                return Optional.of(tokenIdentifier.getIssueDate());
            } catch (Exception e) {
                LOG.warn("Can not decode identifier of token {}, error: {}", token, e);
                return Optional.empty();
            }
        }
        LOG.debug("Unsupported TokenIdentifier kind: {}", identifier.getKind());
        return Optional.empty();
    }
}
