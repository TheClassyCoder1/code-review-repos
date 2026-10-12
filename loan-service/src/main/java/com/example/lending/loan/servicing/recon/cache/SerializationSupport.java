package com.example.lending.loan.servicing.recon.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;

/** Java serialization for cache index entries. */
public final class SerializationSupport {

    private static final ObjectInputFilter KEY_HOOK_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=3;maxrefs=1024;maxarray=4096;maxbytes=262144;"
                    + "com.example.lending.loan.servicing.recon.cache.KeyHook;java.lang.String;!*");

    private SerializationSupport() {
    }

    public static byte[] serialize(Serializable value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(KEY_HOOK_FILTER);
            return (T) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
