package com.acme.claims.service;

import com.acme.claims.config.SnapshotFilters;
import com.acme.claims.model.DraftClaim;
import com.acme.claims.model.LegacyClaim;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClaimSnapshotCodec {

    private static final int MAX_DRAFT_BYTES = 64 * 1024;

    public byte[] encodeDraft(DraftClaim draft) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(draft);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (buffer.size() > MAX_DRAFT_BYTES) {
            throw new IllegalArgumentException("draft too large");
        }
        return buffer.toByteArray();
    }

    public boolean isEmptyDraft(DraftClaim draft) {
        return draft.policyNumber() == null
                && draft.incidentDate() == null
                && (draft.description() == null || draft.description().isBlank())
                && draft.amountClaimed() == null;
    }

    public DraftClaim restoreDraft(byte[] blob) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(blob))) {
            in.setObjectInputFilter(SnapshotFilters.DRAFT_FILTER);
            return (DraftClaim) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("unreadable draft", e);
        }
    }

    public int countValid(List<LegacyClaim> batch) {
        return (int) batch.stream()
                .filter(c -> c.policyNumber() != null && c.amount() != null && c.amount().signum() > 0)
                .count();
    }

    @SuppressWarnings("unchecked")
    public List<LegacyClaim> importLegacyBatch(InputStream upload) {
        try (ObjectInputStream in = new ObjectInputStream(upload)) {
            return (List<LegacyClaim>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("unreadable legacy export", e);
        }
    }
}
