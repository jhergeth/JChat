package name.hergeth.jchat.tools;

import name.hergeth.jchat.ai.context.AmbientContext;

public record ToolContext(
        String conversationId,
        AmbientContext ambientContext,
        String userMessage
) {
    public ToolContext(String conversationId, AmbientContext ambientContext) {
        this(conversationId, ambientContext, "");
    }
}
