package com.example.lending.loan.servicing.statements.render;

import java.util.Map;

/** One render run: local resources and the variables substituted into the command line. */
public class RenderTaskRequest {

    private final ResourceContext resourceContext;
    private final Map<String, String> varPool;

    public RenderTaskRequest(ResourceContext resourceContext, Map<String, String> varPool) {
        this.resourceContext = resourceContext;
        this.varPool = Map.copyOf(varPool);
    }

    public ResourceContext getResourceContext() { return resourceContext; }
    public Map<String, String> getVarPool() { return varPool; }
}
