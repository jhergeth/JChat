package name.hergeth.jchat.ai.model;

import java.time.Instant;

public record Statement(
        String subject,
        String predicate,
        String object,
        String conversationId,
        String turnId,
        Instant createdAt,
        FactSource source
) {
    public Statement(
            String subject,
            String predicate,
            String object,
            String conversationId,
            String turnId,
            Instant createdAt) {
        this(subject, predicate, object, conversationId, turnId, createdAt, FactSource.CHAT);
    }

    public String formatForPrompt() {
        return subject + " | " + predicate + " | " + object;
    }

    public Statement withSource(FactSource source) {
        return new Statement(subject, predicate, object, conversationId, turnId, createdAt, source);
    }
}
