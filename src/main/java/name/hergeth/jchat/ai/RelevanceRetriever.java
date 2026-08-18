package name.hergeth.jchat.ai;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import name.hergeth.jchat.ai.model.Statement;

import java.util.List;

@Singleton
@Replaces(KnowledgeStoreRetriever.class)
public class RelevanceRetriever implements Retriever {

    private final KnowledgeStore knowledgeStore;
    private final KnowledgeLimits limits;

    public RelevanceRetriever(KnowledgeStore knowledgeStore, KnowledgeLimits limits) {
        this.knowledgeStore = knowledgeStore;
        this.limits = limits;
    }

    @Override
    public List<Statement> retrieve(String conversationId, String query) {
        List<Statement> all = knowledgeStore.all(conversationId);
        List<Statement> pool = RetrieverStatementSelector.select(
                all,
                limits.minPromptTurns(),
                limits.minPromptStatements(),
                limits.maxPromptStatements());
        return StatementRelevanceScorer.rank(pool, query, limits.maxKnowledgeInContext());
    }
}
