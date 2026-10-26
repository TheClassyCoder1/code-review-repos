package com.example.lending.ops.part3.workflow;

import java.util.*;

/** Job properties handed to underwriting workflow jobs; always backed by a non-null property set. */
public final class UnderwritingJobContext {
    private final Properties properties = new Properties();
    public UnderwritingJobContext(String contextId, Map<String, String> jobParams) {
        properties.putAll(Objects.requireNonNull(jobParams, "jobParams"));
        properties.put(ContextServiceUtils.CSCommonUtils.CONTEXT_ID_STR, Objects.requireNonNull(contextId, "contextId"));
    }
    public String nodeName() { return ContextServiceUtils.getNodeNameStrByProperties(properties); }
}
