package com.example.lending.loan.servicing.statements.render;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the java command line that runs a statement renderer jar. */
public class StatementRenderCommandBuilder {

    static final String SPACE = " ";

    private final RenderTaskRequest taskRequest;
    private final RenderParameters renderParameters;
    private final Path javaHome;

    public StatementRenderCommandBuilder(RenderTaskRequest taskRequest, RenderParameters renderParameters, Path javaHome) {
        this.taskRequest = taskRequest;
        this.renderParameters = renderParameters;
        this.javaHome = javaHome;
    }

    protected String buildJarCommand() {
        ResourceContext resourceContext = taskRequest.getResourceContext();
        String mainJarAbsolutePathInLocal = resourceContext
                .getResourceItem(renderParameters.getMainJar().getResourceName())
                .getResourceAbsolutePathInLocal();
        StringBuilder builder = new StringBuilder();
        builder.append(getJavaCommandPath())
                .append(SPACE)
                .append(renderParameters.getJvmArgs().trim()).append(SPACE)
                .append(buildResourcePath()).append(SPACE)
                .append("-jar").append(SPACE)
                .append(mainJarAbsolutePathInLocal).append(SPACE)
                .append(renderParameters.getMainArgs().trim());
        return parseParameter(builder.toString());
    }

    private String getJavaCommandPath() {
        return javaHome.resolve("bin").resolve("java").toString();
    }

    private String buildResourcePath() {
        String classpath = renderParameters.getResourceList().stream()
                .map(resource -> taskRequest.getResourceContext().getResourceItem(resource.getResourceName()))
                .filter(item -> item != null)
                .map(ResourceContext.ResourceItem::getResourceAbsolutePathInLocal)
                .collect(Collectors.joining(":"));
        return classpath.isEmpty() ? "" : "-classpath" + SPACE + classpath;
    }

    private String parseParameter(String command) {
        String result = command;
        for (Map.Entry<String, String> variable : taskRequest.getVarPool().entrySet()) {
            result = result.replace("${" + variable.getKey() + "}", variable.getValue());
        }
        return result;
    }
}
