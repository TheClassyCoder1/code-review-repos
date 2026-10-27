package com.example.lending.ops.part2.k8s;

import org.yaml.snakeyaml.Yaml;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the pod templates that teams attach to scoring-model batch jobs. */
public final class PodTemplateParser {

    /** Parses a user-supplied template; only plain YAML gets past {@link YamlTagScreen}. */
    public static Map<String, Object> parse(String content) {
        return loadYamlMap(YamlTagScreen.requirePlainYaml(content));
    }

    private static Map<String, Object> loadYamlMap(String content) {
        Object loaded = new Yaml().load(content);
        if (!(loaded instanceof Map)) {
            return new LinkedHashMap<>();
        }
        return toStringObjectMap((Map<?, ?>) loaded);
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((k, v) -> result.put(String.valueOf(k), v instanceof Map<?, ?> m ? toStringObjectMap(m) : v));
        return result;
    }
}
