package com.example.lending.ops.part2.pretty;

import java.util.regex.Pattern;

/**
 * Hands out formatters for the partner-message viewer. The XML formatter is never exposed directly:
 * input reaches it only once it is known to carry no document type declaration. Without a DTD there
 * are no entities to expand and no external subset to fetch, so the plain parser in {@link XmlPretty}
 * is safe.
 */
public final class PrettyFactory {

    public interface Pretty {
        String format(String source);
    }

    private static final Pattern DTD_MARKUP = Pattern.compile("<!\\s*(DOCTYPE|ENTITY)", Pattern.CASE_INSENSITIVE);
    private static final XmlPretty XML = new XmlPretty();

    public static Pretty forContentType(String contentType) {
        if (contentType == null || !contentType.toLowerCase().contains("xml")) {
            return source -> source;
        }
        return source -> {
            if (source == null || DTD_MARKUP.matcher(source).find()) {
                throw new IllegalArgumentException("XML must be present and carry no document type declaration");
            }
            return XML.format(source);
        };
    }
}
