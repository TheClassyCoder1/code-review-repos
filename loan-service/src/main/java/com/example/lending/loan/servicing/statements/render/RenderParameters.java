package com.example.lending.loan.servicing.statements.render;

import java.util.List;

/** Parameters of one render run, taken from the job definition. */
public class RenderParameters {

    /** Reference to a file in the renderer resource store. */
    public record ResourceInfo(String resourceName) {

        public String getResourceName() {
            return resourceName;
        }
    }

    private final ResourceInfo mainJar;
    private final String jvmArgs;
    private final String mainArgs;
    private final List<ResourceInfo> resourceList;

    public RenderParameters(ResourceInfo mainJar, String jvmArgs, String mainArgs, List<ResourceInfo> resourceList) {
        this.mainJar = mainJar;
        this.jvmArgs = jvmArgs;
        this.mainArgs = mainArgs;
        this.resourceList = List.copyOf(resourceList);
    }


    public ResourceInfo getMainJar() { return mainJar; }
    public String getJvmArgs() { return jvmArgs; }

    public String getMainArgs() { return mainArgs; }
    public List<ResourceInfo> getResourceList() { return resourceList; }
}
