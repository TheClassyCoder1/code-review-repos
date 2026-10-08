package com.example.lending.loan.notify.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class MessageFileUtil {

    private MessageFileUtil() {
    }

    /** Loads {@code input} into {@code props} and always closes the stream. */
    public static void loadProperties(Properties props, InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
    }
}
