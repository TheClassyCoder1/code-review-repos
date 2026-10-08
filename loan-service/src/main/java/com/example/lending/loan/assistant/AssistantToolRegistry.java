package com.example.lending.loan.assistant;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** Read-only loan APIs the borrower assistant may call. */
@Component
public class AssistantToolRegistry {

    public record ApiTool(String name, String title, String path) {
    }

    public record ToolContext(String authorization) {
    }

    public record ToolResult(String content, boolean isError) {
    }

    public record ToolSpecification(ApiTool tool, BiFunction<ToolContext, Map<String, Object>, ToolResult> callHandler) {
    }

    private static final List<ApiTool> TOOLS = List.of(
            new ApiTool("get_loan", "Look up a loan", "/api/v1/loans/{id}"),
            new ApiTool("get_notice_feed", "Recent service announcements", "/api/v1/notices/feed.xml"));

    private final LoanApiInvoker apiInvoker;
    private final Map<String, ToolSpecification> specifications;

    public AssistantToolRegistry(LoanApiInvoker apiInvoker) {
        this.apiInvoker = apiInvoker;
        this.specifications = TOOLS.stream().collect(Collectors.toMap(ApiTool::name, this::createApiTool));
    }

    public Optional<ToolSpecification> find(String name) {
        return Optional.ofNullable(specifications.get(name));
    }

    private ToolSpecification createApiTool(ApiTool tool) {
        return new ToolSpecification(tool, (context, arguments) -> {
            try {
                String authorization = context.authorization();
                String body = apiInvoker.get(tool.path(), arguments, authorization);
                return new ToolResult(body, false);
            } catch (RuntimeException e) {
                return new ToolResult(e.getMessage(), true);
            }
        });
    }
}
