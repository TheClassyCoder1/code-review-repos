package com.example.lending.loan.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class StreamBytes {

    private StreamBytes() {
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
