package name.hergeth.jchat.ai.model;

import java.time.Instant;
import java.util.List;

public record Turn(
        String conversationId,
        String turnId,
        String userMessage,
        String assistantMessage,
        List<ToolResult> toolResults,
        Instant timestamp
) {}
