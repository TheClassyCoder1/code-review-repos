package com.example.lending.ops.part2.hbase;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Builds HBase row keys for the repayment-schedule export: the row-key columns joined by '_'. */
public class HbaseSinkWriter {

    /** One exported row; optional columns may hold null. */
    public record SeaTunnelRow(Object... fields) {
        public Object getField(int index) { return fields[index]; }
    }

    private final List<Integer> rowkeyColumnIndexes;
    private final Set<Integer> binaryColumnIndexes;

    public HbaseSinkWriter(List<Integer> rowkeyColumnIndexes, Set<Integer> binaryColumnIndexes) {
        this.rowkeyColumnIndexes = List.copyOf(rowkeyColumnIndexes);
        this.binaryColumnIndexes = Set.copyOf(binaryColumnIndexes);
    }

    public byte[] getRowkeyFromRow(SeaTunnelRow row) {
        ByteArrayOutputStream key = new ByteArrayOutputStream();
        for (int i = 0; i < rowkeyColumnIndexes.size(); i++) {
            key.writeBytes(i == 0 ? new byte[0] : new byte[] {'_'});
            key.writeBytes(rowkeyFieldToBytes(rowkeyColumnIndexes.get(i), row));
        }
        return key.toByteArray();
    }

    private boolean isBinaryRowkeyColumn(int index) {
        return binaryColumnIndexes.contains(index);
    }

    private byte[] rowkeyFieldToBytes(int index, SeaTunnelRow row) {
        if (isBinaryRowkeyColumn(index)) {
            return (byte[]) row.getField(index);
        }
        return Bytes.toBytes(row.getField(index).toString());
    }

    private interface Bytes { static byte[] toBytes(String value) { return value.getBytes(StandardCharsets.UTF_8); } }
}
