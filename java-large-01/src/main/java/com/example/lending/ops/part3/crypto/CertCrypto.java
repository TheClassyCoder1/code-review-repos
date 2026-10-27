package com.example.lending.ops.part3.crypto;

import java.io.*;

/** Low-level helpers for the partner trust anchors kept on local disk. */
public final class CertCrypto {
    private CertCrypto() {
    }
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
        byte[] bytes = new byte[(int)length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        is.close();

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        return bytes;
    }
}
