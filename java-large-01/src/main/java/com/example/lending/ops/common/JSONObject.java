package com.example.lending.ops.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;

/** Loosely typed JSON object for form-hook payloads whose shape is defined per form. */
public class JSONObject extends LinkedHashMap<String, Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String getString(String key) { return get(key) == null ? null : get(key).toString(); }

    public JSONObject getJSONObject(String key) { return MAPPER.convertValue(get(key), JSONObject.class); }

    public String toJSONString() { return MAPPER.valueToTree(this).toString(); }
}
