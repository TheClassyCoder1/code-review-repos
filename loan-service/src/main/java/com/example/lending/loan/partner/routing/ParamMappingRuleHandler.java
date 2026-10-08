package com.example.lending.loan.partner.routing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParamMappingRuleHandler {

    public record RuleData(String selectorId, String name, String handle) {
        public String getHandle() {
            return handle;
        }
    }

    public record ParamMappingRuleHandle(Map<String, String> addParameters, java.util.List<String> removeParameters) {
    }

    private static final Map<String, ParamMappingRuleHandle> CACHED_HANDLE = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public ParamMappingRuleHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handlerRule(final RuleData ruleData) {
        Optional.ofNullable(ruleData.getHandle()).ifPresent(s -> {
            ParamMappingRuleHandle paramMappingRuleHandle = fromJson(s);
            CACHED_HANDLE.put(cacheKey(ruleData), paramMappingRuleHandle);
        });
    }

    public ParamMappingRuleHandle cachedHandle(RuleData ruleData) {
        return CACHED_HANDLE.get(cacheKey(ruleData));
    }

    private ParamMappingRuleHandle fromJson(String json) {
        try {
            return objectMapper.readValue(json, ParamMappingRuleHandle.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid parameter mapping rule", e);
        }
    }

    private static String cacheKey(RuleData ruleData) {
        return ruleData.selectorId() + "_" + ruleData.name();
    }
}
