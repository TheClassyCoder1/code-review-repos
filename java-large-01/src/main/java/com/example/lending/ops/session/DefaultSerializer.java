package com.example.lending.ops.session;

import lombok.RequiredArgsConstructor;
import java.io.*;

/** Java-serialization codec for console session snapshots; instances are built by SessionSnapshotConfig. */
@RequiredArgsConstructor
public class DefaultSerializer<T> {

    public static class SerializationException extends RuntimeException {
        SerializationException(String message, Throwable cause) { super(message, cause); }
    }

    private final ObjectInputFilter objectInputFilter;

    public T deserialize(byte[] serialized) throws SerializationException {
        if (serialized == null) {
            String msg = "argument cannot be null.";
            throw new IllegalArgumentException(msg);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        BufferedInputStream bis = new BufferedInputStream(bais);
        try {
            ObjectInputStream ois = createObjectInputStream(bis);
            if (objectInputFilter != null) {
                ois.setObjectInputFilter(objectInputFilter);
            }
            @SuppressWarnings({"unchecked"})
            T deserialized = (T) ois.readObject();
            ois.close();
            return deserialized;
        } catch (Exception e) {
            String msg = "Unable to deserialize argument byte array.";
            throw new SerializationException(msg, e);
        }
    }

    protected ObjectInputStream createObjectInputStream(InputStream in) throws IOException {
        return new ObjectInputStream(in);
    }
}
