package name.hergeth.jchat.ai.model;

import java.time.Instant;

public record Statement(
        String subject,
        String predicate,
        String object,
        String conversationId,
        String turnId,
        Instant createdAt
) {
    public String formatForPrompt() {
        return subject + " | " + predicate + " | " + object;
    }
}
