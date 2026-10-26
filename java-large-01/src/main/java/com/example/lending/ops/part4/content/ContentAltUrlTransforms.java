package com.example.lending.ops.part4.content;

import java.util.Map;

/** Builds SEO-friendly URLs for help-centre articles from template macro arguments. */
public class ContentAltUrlTransforms {

    /** Template value wrappers handed to macros by the help-centre renderer. */
    public record SimpleScalar(String getAsString) { }

    public record GenericObjectModel(String getAsString) { }

    public record SimpleNumber(Number getAsNumber) { }

    public record NumberModel(Number getAsNumber) { }

    public String getStringArg(Map<String, Object> args, String key) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            return ((SimpleScalar) o).getAsString();
        } else if (o instanceof GenericObjectModel) {
            return ((GenericObjectModel) o).getAsString();
        } else if (o instanceof SimpleNumber) {
            return ((SimpleNumber) o).getAsNumber().toString();
        } else if (o instanceof NumberModel) {
            return ((NumberModel) o).getAsNumber().toString();
        }
        return null;
    }
}
