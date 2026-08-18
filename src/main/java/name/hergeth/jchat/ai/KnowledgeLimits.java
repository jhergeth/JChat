package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class KnowledgeLimits {

    private final int maxStoreStatements;
    private final int maxPromptStatements;
    private final int minPromptStatements;
    private final int minPromptTurns;
    private final int maxKnowledgeInContext;
    private final int maxSearchInContext;

    public KnowledgeLimits(
            @Value("${app.knowledge.max-statements:500}") int maxStoreStatements,
            @Value("${app.retriever.max-statements:12}") int maxPromptStatements,
            @Value("${app.retriever.min-statements:6}") int minPromptStatements,
            @Value("${app.retriever.min-turns:3}") int minPromptTurns,
            @Value("${app.retriever.max-knowledge:12}") int maxKnowledgeInContext,
            @Value("${app.retriever.max-search:6}") int maxSearchInContext) {
        this.maxStoreStatements = Math.max(1, maxStoreStatements);
        this.maxPromptStatements = Math.max(1, maxPromptStatements);
        this.minPromptStatements = Math.max(0, minPromptStatements);
        this.minPromptTurns = Math.max(0, minPromptTurns);
        this.maxKnowledgeInContext = Math.max(1, maxKnowledgeInContext);
        this.maxSearchInContext = Math.max(1, maxSearchInContext);
    }

    public int maxStoreStatements() {
        return maxStoreStatements;
    }

    public int maxPromptStatements() {
        return maxPromptStatements;
    }

    public int minPromptStatements() {
        return Math.min(minPromptStatements, maxPromptStatements);
    }

    public int minPromptTurns() {
        return minPromptTurns;
    }

    public int maxKnowledgeInContext() {
        return maxKnowledgeInContext;
    }

    public int maxSearchInContext() {
        return maxSearchInContext;
    }
}
