package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.ToolResult;
import name.hergeth.jchat.ai.model.Turn;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TurnFactory {

    private TurnFactory() {}

    public static Turn fromExchange(String conversationId, List<ChatMessage> messages, String assistantReply) {
        return new Turn(
                conversationId,
                UUID.randomUUID().toString(),
                lastUserMessage(messages),
                assistantReply,
                toolResults(messages),
                Instant.now(),
                TurnRenderer.renderConversation(messages, assistantReply));
    }

    private static String lastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("user".equals(msg.role())) {
                return msg.content();
            }
        }
        throw new IllegalArgumentException("no user message found");
    }

    private static List<ToolResult> toolResults(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> "tool".equals(m.role()))
                .map(m -> new ToolResult("tool", m.content()))
                .toList();
    }
}
