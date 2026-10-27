package com.example.lending.ops.part2.cache;

import lombok.extern.slf4j.Slf4j;
import java.io.*;

/** JDK serialization for agent-session cache values; only {@link SignedCacheCodec} uses it. */
final class SaJdkSerializer {

    byte[] valueToBytes(Serializable value) {
        return org.springframework.util.SerializationUtils.serialize(value);
    }

    public Object valueFromBytes(byte[] bytes) {
        if(bytes == null || bytes.length == 0)
            return null;

        ObjectInputStream objectInput = null;
        try {
            ByteArrayInputStream bytesInput = new ByteArrayInputStream(bytes);
            objectInput = new ObjectInputStream(bytesInput);
            return objectInput.readObject();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if (objectInput != null)
                try {objectInput.close();} catch (Exception e) {LogKit.error(e.getMessage(), e);}
        }
    }

    @Slf4j
    private static final class LogKit {
        static void error(String message, Throwable t) { log.error(message, t); }
    }
}
