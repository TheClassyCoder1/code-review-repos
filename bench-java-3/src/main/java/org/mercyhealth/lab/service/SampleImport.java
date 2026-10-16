package org.mercyhealth.lab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SampleImport {

    private static final int MAX_SNAPSHOT_BYTES = 512 * 1024;

    public record SampleRecord(String accession, String specimenType, String collectedBy,
                               Instant collectedAt) implements Serializable {
    }

    public record SampleSnapshot(long orderId, ArrayList<SampleRecord> samples) implements Serializable {
    }

    @SuppressWarnings("unchecked")
    public List<SampleRecord> readAnalyzerBatch(InputStream upload) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(upload)) {
            Object batch = in.readObject();
            if (!(batch instanceof List<?> list)) {
                throw new IOException("analyzer batch is not a sample list");
            }
            return (List<SampleRecord>) list;
        } catch (ClassNotFoundException e) {
            throw new IOException("unknown class in analyzer batch", e);
        }
    }

    public static String describe(SampleRecord sample) {
        return sample.accession() + " (" + sample.specimenType() + ")";
    }

    public static boolean isRecent(SampleRecord sample, Instant now) {
        return sample.collectedAt() != null && sample.collectedAt().isAfter(now.minusSeconds(86_400));
    }

    public SampleSnapshot readSnapshot(byte[] stored) throws IOException {
        try (ObjectInputStream in = new AllowListObjectInputStream(new ByteArrayInputStream(stored))) {
            Object snapshot = in.readObject();
            if (!(snapshot instanceof SampleSnapshot typed)) {
                throw new IOException("stored snapshot has unexpected type");
            }
            return typed;
        } catch (ClassNotFoundException e) {
            throw new IOException("unknown class in sample snapshot", e);
        }
    }

    public int countForSpecimen(SampleSnapshot snapshot, String specimenType) {
        int count = 0;
        for (SampleRecord sample : snapshot.samples()) {
            if (specimenType.equals(sample.specimenType())) {
                count++;
            }
        }
        return count;
    }

    public byte[] writeSnapshot(long orderId, List<SampleRecord> samples) throws IOException {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("snapshot needs at least one sample");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new SampleSnapshot(orderId, new ArrayList<>(samples)));
        }
        if (bytes.size() > MAX_SNAPSHOT_BYTES) {
            throw new IOException("sample snapshot too large: " + bytes.size() + " bytes");
        }
        return bytes.toByteArray();
    }
}
