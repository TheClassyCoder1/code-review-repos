package com.example.lending.ops.part5.common;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Decodes Base64 + deflate compressed report layouts stored in text columns. */
public final class DeflaterUtils {

    private static final int BUFFER_SIZE = 4096;

    private DeflaterUtils() {
    }

    public static String unzipString(String compressedText) {
        if (compressedText == null) {
            return null;
        }
        if (compressedText.isEmpty()) {
            return "";
        }

        byte[] compressedBytes;
        try {
            compressedBytes = Base64.getDecoder().decode(compressedText);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream output = new ByteArrayOutputStream(BUFFER_SIZE);
        try {
            while (!inflater.finished()) {
                int length = inflater.inflate(buffer);
                if (length > 0) {
                    output.write(buffer, 0, length);
                    continue;
                }
                if (inflater.finished()) {
                    break;
                }
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    return null;
                }
                // Inflater made no progress and cannot request more input. Treat it as corrupt.
                return null;
            }
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
