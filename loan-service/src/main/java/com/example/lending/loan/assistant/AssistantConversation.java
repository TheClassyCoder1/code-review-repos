package com.example.lending.loan.assistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Message history of one assistant turn. */
public class AssistantConversation {

    public record ChatMessage(String role, String toolCallId, String content) {
    }

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addUserMessage(String content) {
        messages.add(new ChatMessage("user", null, content));
    }

    public void addToolResult(String toolCallId, String content) {
        messages.add(
                new ChatMessage(
                        "tool",
                        toolCallId,
                        content));
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
