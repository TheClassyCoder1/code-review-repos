package com.example.lending.ops.part5.gateway;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Request rewrite rule applied by the partner gateway before a call reaches
 * the lending services, e.g. stripping legacy query parameters.
 */
@Getter
@Setter
public class RequestHandle {

    private ShenyuRequestParameter parameter = new ShenyuRequestParameter();

    /** Query-parameter rewrites. Holds parameter names and literal values only. */
    @Getter
    @Setter
    public static class ShenyuRequestParameter {

        private Map<String, String> addParameters = new HashMap<>();
        private Map<String, String> replaceParameterKeys = new HashMap<>();
        private Map<String, String> setParameters = new HashMap<>();
        private Set<String> removeParameterKeys = new HashSet<>();

        @Override
        public String toString() {
            return "ShenyuRequestParameter{"
                    + "addParameters="
                    + addParameters
                    + ", replaceParameterKeys="
                    + replaceParameterKeys
                    + ", setParameters="
                    + setParameters
                    + ", removeParameterKeys="
                    + removeParameterKeys
                    + '}';
        }
    }
}
