package com.example.lending.loan.assistant;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Runs the tool calls of one assistant turn. Every request gets a fresh conversation. */
@RestController
public class AssistantSession {

    static final int MAX_TOOL_CALLS = 8;
    static final int MAX_CONTENT_CHARS = 4_000;

    public record ToolCall(String id, String name, Map<String, Object> arguments) {
    }

    public record TurnRequest(String prompt, List<ToolCall> toolCalls) {
    }

    private final AssistantToolRegistry registry;

    public AssistantSession(AssistantToolRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/api/v1/assistant/turns")
    public List<AssistantConversation.ChatMessage> runTurn(@RequestBody TurnRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AssistantConversation conversation = new AssistantConversation();
        conversation.addUserMessage(truncate(request.prompt()));
        List<ToolCall> calls = request.toolCalls() == null ? List.of() : request.toolCalls();
        AssistantToolRegistry.ToolContext context = new AssistantToolRegistry.ToolContext(authorization);
        for (ToolCall call : calls.subList(0, Math.min(calls.size(), MAX_TOOL_CALLS))) {
            String content = registry.find(call.name())
                    .map(spec -> spec.callHandler().apply(context, call.arguments()).content())
                    .orElse("Unknown tool");
            conversation.addToolResult(call.id(), truncate(content));
        }
        return conversation.getMessages();
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= MAX_CONTENT_CHARS ? content : content.substring(0, MAX_CONTENT_CHARS);
    }
}
