package name.hergeth.jchat.ai;

import name.hergeth.jchat.ai.model.ToolResult;
import name.hergeth.jchat.ai.model.Turn;
import name.hergeth.jchat.openai.dto.ChatMessage;

import java.util.List;
import java.util.Locale;

public final class TurnRenderer {

    private TurnRenderer() {}

    public static String render(Turn turn) {
        return turn.conversationForExtraction();
    }

    public static String renderConversation(List<ChatMessage> messages, String assistantReply) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            if ("system".equals(message.role())) {
                continue;
            }
            String content = message.content() == null ? "" : message.content();
            if (content.isBlank()) {
                continue;
            }
            sb.append(label(message.role())).append(": ")
                    .append(content).append('\n');
        }
        sb.append("Assistant: ").append(assistantReply);
        return sb.toString().trim();
    }

    private static String label(String role) {
        return switch (role) {
            case "user" -> "User";
            case "assistant" -> "Assistant";
            case "tool" -> "Tool";
            default -> role.substring(0, 1).toUpperCase(Locale.ROOT) + role.substring(1);
        };
    }
}
