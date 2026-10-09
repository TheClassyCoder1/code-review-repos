package com.example.lending.loan.servicing.recon.cache;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/** Index entry of a chunked cache value: the chunk keys and the total value length. */
public final class KeyHook implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String[] chunkKeys;
    private final long valueLength;

    public KeyHook(String[] chunkKeys, long valueLength) {
        this.chunkKeys = Arrays.copyOf(chunkKeys, chunkKeys.length);
        this.valueLength = valueLength;
    }

    public String[] getChunkKeys() {
        return Arrays.copyOf(chunkKeys, chunkKeys.length);
    }

    public long getValueLength() {
        return valueLength;
    }
}
