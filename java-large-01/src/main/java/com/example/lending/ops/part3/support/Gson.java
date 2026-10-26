package com.example.lending.ops.part3.support;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.*;

/** Gson-style facade over Jackson for payload code ported from the broker and gateway admin tools. */
public class Gson {
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public <T> T fromJson(String json, Class<T> type) {
        if (json == null) return null;
        if (JsonElement.class.isAssignableFrom(type)) return type.cast(JsonElement.wrap(io(() -> MAPPER.readTree(json))));
        return io(() -> MAPPER.readValue(json, type));
    }
    public <T> T fromJson(JsonElement element, Class<T> type) { return element == null ? null : fromJson(element.toString(), type); }
    public <T> T fromJson(String json, Type type) { return io(() -> MAPPER.readValue(json, MAPPER.getTypeFactory().constructType(type))); }
    public String toJson(Object value) { return io(() -> MAPPER.writeValueAsString(value)); }
    private interface Io<T> { T get() throws Exception; }
    private static <T> T io(Io<T> call) { try { return call.get(); } catch (Exception e) { throw new IllegalArgumentException("malformed json payload", e); } }
    public static class JsonElement {
        final JsonNode node;
        JsonElement(JsonNode node) { this.node = node; }
        static JsonElement wrap(JsonNode n) { return n instanceof ObjectNode ? new JsonObject(n) : n instanceof ArrayNode ? new JsonArray(n) : new JsonElement(n); }
        @Override public String toString() { return node.toString(); }
    }
    public static class JsonObject extends JsonElement {
        JsonObject(JsonNode node) { super(node); }
        public JsonElement get(String member) { return node.has(member) ? wrap(node.get(member)) : null; }
    }
    public static class JsonArray extends JsonElement { JsonArray(JsonNode node) { super(node); } }
    /** Captures a generic type: {@code new TypeToken<List<X>>() {}.getType()}. */
    public abstract static class TypeToken<T> {
        public Type getType() { return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]; }
    }
}
